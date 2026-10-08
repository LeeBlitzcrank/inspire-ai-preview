package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.config.SupportProperties;
import com.inspire.platform.rag.model.SupportModels.SupportChunkDocument;
import com.inspire.platform.rag.model.SupportModels.SupportKnowledgeHit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SupportKnowledgeStore {

    private final SupportProperties properties;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private RestClient client;

    public SupportKnowledgeStore(SupportProperties properties,
                                 RagProperties ragProperties,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) return;
        String host = ragProperties.getElasticsearchHost();
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
            // create below
        }
        if (exists) {
            try {
                Response response = client.performRequest(
                        new Request("GET", "/" + properties.getIndexName() + "/_mapping"));
                JsonNode mapping = objectMapper.readTree(response.getEntity().getContent());
                String type = mapping.path(properties.getIndexName())
                        .path("mappings").path("properties").path("embedding").path("type").asText("");
                int dims = mapping.path(properties.getIndexName())
                        .path("mappings").path("properties").path("embedding").path("dims").asInt(0);
                if ("dense_vector".equals(type) && dims == ragProperties.getEmbeddingDims()) {
                    return;
                }
                log.warn("客服知识库 embedding mapping 不匹配（type={}, dims={}），删除重建",
                        type, dims);
                client.performRequest(new Request("DELETE", "/" + properties.getIndexName()));
            } catch (Exception e) {
                log.warn("客服知识库 mapping 校验失败，将重建: {}", e.getMessage());
            }
        }
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("chunk_id", Map.of("type", "keyword"));
            fields.put("doc_id", Map.of("type", "keyword"));
            fields.put("title", Map.of("type", "text"));
            fields.put("section", Map.of("type", "text"));
            fields.put("content", Map.of("type", "text"));
            fields.put("file_path", Map.of("type", "keyword"));
            fields.put("updated_at", Map.of("type", "keyword", "index", false));
            fields.put("embedding", Map.of(
                    "type", "dense_vector",
                    "dims", ragProperties.getEmbeddingDims()
            ));
            Map<String, Object> mapping = Map.of(
                    "settings", Map.of("number_of_shards", 1, "number_of_replicas", 0),
                    "mappings", Map.of("properties", fields)
            );
            Request request = new Request("PUT", "/" + properties.getIndexName());
            request.setEntity(new NStringEntity(
                    objectMapper.writeValueAsString(mapping), ContentType.APPLICATION_JSON));
            client.performRequest(request);
            log.info("客服知识库索引已创建: {}", properties.getIndexName());
        } catch (Exception e) {
            log.warn("客服知识库索引创建失败: {}", e.getMessage());
        }
    }

    public void replaceAll(List<SupportChunkDocument> chunks) {
        if (!properties.isEnabled()) return;
        try {
            try {
                client.performRequest(new Request("DELETE", "/" + properties.getIndexName()));
            } catch (Exception ignored) {
                // first build
            }
            Thread.sleep(300);
            ensureIndex();
            if (chunks == null || chunks.isEmpty()) return;

            StringBuilder body = new StringBuilder();
            for (SupportChunkDocument chunk : chunks) {
                body.append(objectMapper.writeValueAsString(Map.of(
                        "index", Map.of("_index", properties.getIndexName(), "_id", chunk.chunkId())
                ))).append('\n');
                body.append(objectMapper.writeValueAsString(toDocument(chunk))).append('\n');
            }
            Request request = new Request("POST", "/_bulk?refresh=true");
            request.setEntity(new NStringEntity(body.toString(), ContentType.APPLICATION_JSON));
            Response response = client.performRequest(request);
            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            if (result.path("errors").asBoolean(false)) {
                throw new IllegalStateException("客服知识库 bulk 写入包含失败项");
            }
        } catch (Exception e) {
            throw new IllegalStateException("客服知识库写入失败: " + e.getMessage(), e);
        }
    }

    public List<SupportKnowledgeHit> searchVector(float[] vector, int limit) {
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
            log.warn("客服知识库向量检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SupportKnowledgeHit> searchKeyword(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", limit);
            body.put("track_total_hits", false);
            body.put("_source", sourceFields());
            body.put("query", Map.of("bool", Map.of(
                    "must", List.of(Map.of("multi_match", Map.of(
                            "query", query,
                            "fields", List.of("title^5", "section^3", "content", "file_path^2"),
                            "type", "best_fields",
                            "operator", "or"
                    )))
            )));
            return parseHits(postSearch(body));
        } catch (Exception e) {
            log.warn("客服知识库关键词检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public long count() {
        try {
            Response response = client.performRequest(
                    new Request("GET", "/" + properties.getIndexName() + "/_count"));
            return objectMapper.readTree(response.getEntity().getContent()).path("count").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private JsonNode postSearch(Map<String, Object> body) throws Exception {
        Request request = new Request("POST", "/" + properties.getIndexName() + "/_search");
        request.setEntity(new NStringEntity(
                objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
        return objectMapper.readTree(client.performRequest(request).getEntity().getContent());
    }

    private List<SupportKnowledgeHit> parseHits(JsonNode response) {
        List<SupportKnowledgeHit> hits = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            hits.add(new SupportKnowledgeHit(
                    source.path("chunk_id").asText(""),
                    source.path("doc_id").asText(""),
                    source.path("title").asText(""),
                    source.path("section").asText(""),
                    source.path("content").asText(""),
                    source.path("file_path").asText(""),
                    source.path("updated_at").asText(""),
                    hit.path("_score").asDouble(0)
            ));
        }
        hits.sort(Comparator.comparingDouble(SupportKnowledgeHit::score).reversed());
        return hits;
    }

    private Map<String, Object> toDocument(SupportChunkDocument chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", chunk.chunkId());
        doc.put("doc_id", chunk.docId());
        doc.put("title", chunk.title());
        doc.put("section", chunk.section());
        doc.put("content", chunk.content());
        doc.put("file_path", chunk.filePath());
        doc.put("updated_at", chunk.updatedAt());
        doc.put("embedding", toList(chunk.embedding()));
        return doc;
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) values.add(value);
        return values;
    }

    private List<String> sourceFields() {
        return List.of("chunk_id", "doc_id", "title", "section", "content",
                "file_path", "updated_at");
    }
}
