package com.inspire.platform.search.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("esSearchService")
public class EsSearchService implements SearchService {

    private RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String esHosts;
    private static final String INDEX = "inspire_index";

    public EsSearchService(ObjectMapper objectMapper,
                           @Value("${inspire.es.hosts:localhost:9200}") String esHosts) {
        this.objectMapper = objectMapper;
        this.esHosts = esHosts;
    }

    private RestClient getClient() {
        if (restClient == null) {
            String host = esHosts.contains("://") ? esHosts : "http://" + esHosts;
            restClient = RestClient.builder(org.apache.http.HttpHost.create(host)).build();
        }
        return restClient;
    }

    @Override
    public List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter) {
        try {
            Request req = new Request("POST", "/" + INDEX + "/_search");
            req.setJsonEntity(buildQuery(keyword, tag, (page - 1) * size, size, searchAfter));
            Response resp = getClient().performRequest(req);
            String json = EntityUtils.toString(resp.getEntity());
            JsonNode root = objectMapper.readTree(json);

            List<SearchResultVO> results = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                JsonNode src = hit.path("_source");
                SearchResultVO vo = new SearchResultVO();
                vo.setId(src.path("id").asLong());
                vo.setTitle(src.path("title").asText());
                vo.setImg(src.path("img").asText());
                vo.setTag(src.path("tag").asText());
                if (src.hasNonNull("category_id")) vo.setCategoryId(src.path("category_id").asLong());
                if (src.hasNonNull("sub_category_id")) vo.setSubCategoryId(src.path("sub_category_id").asLong());
                vo.setHeat(src.path("heat").asInt());
                vo.setViewCount(src.path("view_count").asLong());
                vo.setLikeCount(src.path("like_count").asInt());
                vo.setCollectCount(src.path("collect_count").asInt());
                vo.setPublishCity(src.path("publish_city").asText());
                String ct = src.path("create_time").asText();
                if (!ct.isEmpty()) {
                    vo.setCreateTime(LocalDateTime.parse(ct, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                vo.setSource("es");
                results.add(vo);
            }
            log.debug("ES搜索: keyword={}, tag={}, hits={}", keyword, tag, results.size());
            return results;
        } catch (Exception e) {
            log.warn("ES搜索异常: {}", e.getMessage());
            throw new RuntimeException("ES不可用", e);
        }
    }

    /**
     * 构建 ES 查询 JSON
     * 使用 bool+should 替代 multi_match，兼容性更好
     */
    private String buildQuery(String keyword, String tag, int from, int size, String searchAfter) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> bool = new LinkedHashMap<>();
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("status", 1)));
        filters.add(Map.of("term", Map.of("deleted", 0)));
        if (tag != null && !tag.isBlank()) {
            filters.add(Map.of("term", Map.of("tag", tag)));
        }
        bool.put("filter", filters);
        bool.put("should", List.of(
                Map.of("match", Map.of("title", Map.of("query", keyword, "boost", 10))),
                Map.of("match", Map.of("tag", Map.of("query", keyword, "boost", 5)))
        ));
        bool.put("minimum_should_match", 1);
        root.put("query", Map.of("bool", bool));
        root.put("size", size);
        root.put("track_total_hits", false);
        root.put("_source", List.of(
                "id", "title", "img", "tag", "category_id", "sub_category_id",
                "heat", "view_count", "like_count", "collect_count",
                "publish_city", "create_time"
        ));
        root.put("sort", List.of(
                Map.of("heat", Map.of("order", "desc")),
                Map.of("id", Map.of("order", "asc"))
        ));
        if (searchAfter != null && !searchAfter.isBlank()) {
            String[] parts = searchAfter.split("_", 2);
            if (parts.length == 2) {
                root.put("search_after", List.of(
                        Integer.parseInt(parts[0]),
                        Long.parseLong(parts[1])
                ));
            }
        } else {
            root.put("from", from);
        }
        return objectMapper.writeValueAsString(root);
    }
}
