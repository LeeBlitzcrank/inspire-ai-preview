/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/RecommendationVectorService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：查询推荐向量索引，并构建用户兴趣向量
 * 维护说明：向量索引不可用时必须静默降级到热度和其他规则召回。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecommendationVectorService {

    private final ObjectMapper objectMapper;
    private final String elasticsearchUri;
    private final String indexName;
    private final int dims;
    private RestClient client;

    public RecommendationVectorService(
            ObjectMapper objectMapper,
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String elasticsearchUri,
            @Value("${inspire.recommend.vector-index:inspire_recommend_vector}") String indexName,
            @Value("${inspire.recommend.embedding-dims:768}") int dims) {
        this.objectMapper = objectMapper;
        this.elasticsearchUri = elasticsearchUri.split(",")[0].trim();
        this.indexName = indexName;
        this.dims = dims;
    }

    @PostConstruct
    public void init() {
        try {
            String uri = elasticsearchUri.contains("://")
                    ? elasticsearchUri : "http://" + elasticsearchUri;
            client = RestClient.builder(HttpHost.create(uri)).build();
        } catch (Exception e) {
            log.warn("推荐向量客户端初始化失败: {}", e.getMessage());
        }
    }

    public Map<Long, Double> search(float[] vector, int limit) {
        if (client == null || vector == null || vector.length != dims) return Map.of();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", Math.max(1, Math.min(limit, 500)));
            body.put("track_total_hits", false);
            body.put("_source", List.of("inspire_id"));
            body.put("query", Map.of("script_score", Map.of(
                    "query", Map.of("exists", Map.of("field", "embedding")),
                    "script", Map.of(
                            "source", "cosineSimilarity(params.vector, 'embedding') + 1.0",
                            "params", Map.of("vector", toList(vector))
                    )
            )));
            Request request = new Request("POST", "/" + indexName + "/_search");
            request.setEntity(new NStringEntity(
                    objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            Response response = client.performRequest(request);
            JsonNode json = objectMapper.readTree(response.getEntity().getContent());
            Map<Long, Double> scores = new LinkedHashMap<>();
            for (JsonNode hit : json.path("hits").path("hits")) {
                long inspireId = hit.path("_source").path("inspire_id").asLong(0);
                if (inspireId > 0) scores.put(inspireId, hit.path("_score").asDouble(0));
            }
            return scores;
        } catch (Exception e) {
            log.debug("推荐向量召回不可用: {}", e.getMessage());
            return Map.of();
        }
    }

    public float[] average(List<Long> inspireIds) {
        if (client == null || inspireIds == null || inspireIds.isEmpty()) return null;
        try {
            List<Long> ids = inspireIds.stream().distinct().limit(120).toList();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", ids.size());
            body.put("_source", List.of("embedding"));
            body.put("query", Map.of("terms", Map.of("inspire_id", ids)));
            Request request = new Request("POST", "/" + indexName + "/_search");
            request.setEntity(new NStringEntity(
                    objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            Response response = client.performRequest(request);
            JsonNode json = objectMapper.readTree(response.getEntity().getContent());
            float[] sum = new float[dims];
            int count = 0;
            for (JsonNode hit : json.path("hits").path("hits")) {
                JsonNode embedding = hit.path("_source").path("embedding");
                if (!embedding.isArray() || embedding.size() != dims) continue;
                for (int i = 0; i < dims; i++) {
                    sum[i] += (float) embedding.get(i).asDouble();
                }
                count++;
            }
            if (count == 0) return null;
            normalize(sum);
            return sum;
        } catch (Exception e) {
            log.debug("用户兴趣向量构建不可用: {}", e.getMessage());
            return null;
        }
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) values.add(value);
        return values;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) sum += value * value;
        double norm = Math.sqrt(sum);
        if (norm <= 1e-9) return;
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
