package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.ChunkDocument;
import com.inspire.platform.rag.model.RagModels.SearchHit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ElasticsearchRagStore {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private RestClient client;

    public ElasticsearchRagStore(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) return;
        String host = properties.getElasticsearchHost();
        if (!host.contains("://")) host = "http://" + host;
        client = RestClient.builder(HttpHost.create(host)).build();
        ensureIndex();
    }

    public void ensureIndex() {
        boolean exists = false;
        try {
            client.performRequest(new Request("HEAD", "/" + properties.getIndexName()));
            exists = true;
        } catch (Exception ignored) {
            // 索引不存在时继续创建
        }
        if (exists) {
            try {
                Request mappingRequest = new Request("GET", "/" + properties.getIndexName() + "/_mapping");
                Response response = client.performRequest(mappingRequest);
                JsonNode mapping = objectMapper.readTree(response.getEntity().getContent());
                String embeddingType = mapping.path(properties.getIndexName())
                        .path("mappings")
                        .path("properties")
                        .path("embedding")
                        .path("type")
                        .asText("");
                int embeddingDims = mapping.path(properties.getIndexName())
                        .path("mappings")
                        .path("properties")
                        .path("embedding")
                        .path("dims")
                        .asInt(0);
                if ("dense_vector".equals(embeddingType)
                        && embeddingDims == properties.getEmbeddingDims()) {
                    return;
                }
                log.warn("RAG 索引 embedding mapping 不匹配（type={}, dims={}, expectedDims={}），自动删除重建",
                        embeddingType, embeddingDims, properties.getEmbeddingDims());
                client.performRequest(new Request("DELETE", "/" + properties.getIndexName()));
            } catch (Exception e) {
                log.warn("RAG 索引 mapping 校验失败，将尝试重建: {}", e.getMessage());
            }
        }
        try {
            Map<String, Object> mapping = new LinkedHashMap<>();
            mapping.put("settings", Map.of("number_of_shards", 1, "number_of_replicas", 0));
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("inspire_id", Map.of("type", "long"));
            fields.put("chunk_id", Map.of("type", "keyword"));
            fields.put("modality", Map.of("type", "keyword"));
            fields.put("title", Map.of("type", "text"));
            fields.put("content", Map.of("type", "text"));
            fields.put("caption", Map.of("type", "text"));
            fields.put("tag", Map.of("type", "keyword"));
            fields.put("city", Map.of("type", "keyword"));
            fields.put("author_id", Map.of("type", "long"));
            fields.put("series_id", Map.of("type", "long"));
            fields.put("image_url", Map.of("type", "keyword", "index", false));
            fields.put("embedding", Map.of(
                    "type", "dense_vector",
                    "dims", properties.getEmbeddingDims()
            ));
            mapping.put("mappings", Map.of("properties", fields));
            Request request = new Request("PUT", "/" + properties.getIndexName());
            request.setEntity(new NStringEntity(objectMapper.writeValueAsString(mapping), ContentType.APPLICATION_JSON));
            client.performRequest(request);
            log.info("RAG ES 索引已创建: {}", properties.getIndexName());
        } catch (Exception e) {
            log.warn("RAG ES 索引创建失败: {}", e.getMessage());
        }
    }

    public void replaceDocument(long inspireId, List<ChunkDocument> chunks) {
        deleteDocument(inspireId);
        if (chunks == null || chunks.isEmpty()) return;
        try {
            StringBuilder body = new StringBuilder();
            for (ChunkDocument chunk : chunks) {
                body.append(objectMapper.writeValueAsString(Map.of(
                        "index", Map.of("_index", properties.getIndexName(), "_id", chunk.chunkId())
                ))).append('\n');
                body.append(objectMapper.writeValueAsString(toDocument(chunk))).append('\n');
            }
            Request request = new Request("POST", "/_bulk?refresh=false");
            request.setEntity(new NStringEntity(body.toString(), ContentType.APPLICATION_JSON));
            Response response = client.performRequest(request);
            JsonNode json = objectMapper.readTree(response.getEntity().getContent());
            if (json.path("errors").asBoolean(false)) {
                log.warn("RAG bulk 写入包含失败项 inspireId={}", inspireId);
            }
        } catch (Exception e) {
            log.warn("RAG 文档写入失败 inspireId={}: {}", inspireId, e.getMessage());
        }
    }

    public void deleteDocument(long inspireId) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "query", Map.of("term", Map.of("inspire_id", inspireId)),
                    "conflicts", "proceed"
            ));
            Request request = new Request("POST",
                    "/" + properties.getIndexName() + "/_delete_by_query?refresh=false&ignore_unavailable=true");
            request.setEntity(new NStringEntity(body, ContentType.APPLICATION_JSON));
            client.performRequest(request);
        } catch (Exception e) {
            log.debug("RAG 删除旧文档失败 inspireId={}: {}", inspireId, e.getMessage());
        }
    }

    public List<SearchHit> searchVector(float[] vector, int limit) {
        if (vector == null || vector.length == 0) return List.of();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", limit);
            body.put("track_total_hits", false);
            body.put("_source", sourceFields());
            body.put("query", Map.of("script_score", Map.of(
                    "query", Map.of("match_all", Map.of()),
                    "script", Map.of(
                            "source", "cosineSimilarity(params.vector, 'embedding') + 1.0",
                            "params", Map.of("vector", toList(vector))
                    )
            )));
            return parseHits(postSearch(body));
        } catch (Exception e) {
            log.warn("RAG 向量检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SearchHit> searchKeyword(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", limit);
            body.put("track_total_hits", false);
            body.put("_source", sourceFields());
            body.put("query", Map.of("bool", Map.of(
                    "must", List.of(Map.of("multi_match", Map.of(
                            "query", query,
                            "fields", List.of("title^4", "caption^3", "content", "tag^3"),
                            "type", "best_fields",
                            "operator", "or"
                    )))
            )));
            return parseHits(postSearch(body));
        } catch (Exception e) {
            log.warn("RAG 关键词检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public long count() {
        try {
            Request request = new Request("GET", "/" + properties.getIndexName() + "/_count");
            Response response = client.performRequest(request);
            JsonNode json = objectMapper.readTree(response.getEntity().getContent());
            return json.path("count").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean available() {
        try {
            client.performRequest(new Request("GET", "/_cluster/health"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private JsonNode postSearch(Map<String, Object> body) throws Exception {
        Request request = new Request("POST", "/" + properties.getIndexName() + "/_search");
        request.setEntity(new NStringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
        Response response = client.performRequest(request);
        return objectMapper.readTree(response.getEntity().getContent());
    }

    private List<SearchHit> parseHits(JsonNode response) {
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            hits.add(new SearchHit(
                    source.path("inspire_id").asLong(),
                    source.path("chunk_id").asText(""),
                    source.path("modality").asText("text"),
                    source.path("title").asText(""),
                    source.path("content").asText(""),
                    source.path("caption").asText(""),
                    source.path("tag").asText(""),
                    source.path("image_url").asText(""),
                    hit.path("_score").asDouble(0)
            ));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        return hits;
    }

    private Map<String, Object> toDocument(ChunkDocument chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("inspire_id", chunk.inspireId());
        doc.put("chunk_id", chunk.chunkId());
        doc.put("modality", chunk.modality());
        doc.put("title", chunk.title());
        doc.put("content", chunk.content());
        doc.put("caption", chunk.caption());
        doc.put("tag", chunk.tag());
        doc.put("city", chunk.city());
        doc.put("author_id", chunk.authorId());
        doc.put("series_id", chunk.seriesId());
        doc.put("image_url", chunk.imageUrl());
        doc.put("embedding", toList(chunk.embedding()));
        return doc;
    }

    private List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) list.add(value);
        return list;
    }

    private List<String> sourceFields() {
        return List.of("inspire_id", "chunk_id", "modality", "title", "content",
                "caption", "tag", "image_url");
    }
}
