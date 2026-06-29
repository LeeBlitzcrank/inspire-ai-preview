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
import java.util.List;

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
    public List<SearchResultVO> search(String keyword, String tag, int page, int size) {
        try {
            Request req = new Request("POST", "/" + INDEX + "/_search");
            req.setJsonEntity(buildQuery(keyword, tag, (page - 1) * size, size));
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
                vo.setHeat(src.path("heat").asInt());
                vo.setViewCount(src.path("view_count").asLong());
                vo.setLikeCount(src.path("like_count").asInt());
                vo.setCollectCount(src.path("collect_count").asInt());
                vo.setPublishCity(src.path("publish_city").asText());
                String ct = src.path("create_time").asText();
                if (!ct.isEmpty()) vo.setCreateTime(LocalDateTime.parse(ct, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                vo.setSource("es");
                results.add(vo);
            }
            log.info("ES搜索: keyword={}, tag={}, hits={}", keyword, tag, results.size());
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
    private String buildQuery(String keyword, String tag, int from, int size) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"from\":").append(from).append(",\"size\":").append(size).append(",");
        sb.append("\"query\":{\"bool\":{\"should\":[");
        // title 匹配（权重 10）
        sb.append("{\"match\":{\"title\":{\"query\":\"").append(esc(keyword)).append("\",\"boost\":10}}}");
        // tag 匹配（权重 5）
        sb.append(",{\"match\":{\"tag\":{\"query\":\"").append(esc(keyword)).append("\",\"boost\":5}}}");
        // 分类筛选
        if (tag != null && !tag.isEmpty()) {
            sb.append(",{\"term\":{\"tag\":\"").append(esc(tag)).append("\"}}");
        }
        sb.append("]}}");
        sb.append(",\"sort\":[{\"heat\":{\"order\":\"desc\"}}]}");
        return sb.toString();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
