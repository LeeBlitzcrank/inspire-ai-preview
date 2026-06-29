package com.inspire.platform.core.service.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.core.entity.InspireMain;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class EsSyncService {

    private RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String esHosts;
    private static final String INDEX = "inspire_index";

    public EsSyncService(ObjectMapper objectMapper,
                         @Value("${inspire.es.hosts:}") String esHosts) {
        this.objectMapper = objectMapper;
        this.esHosts = esHosts;
    }

    private boolean isEnabled() {
        return esHosts != null && !esHosts.isEmpty();
    }

    private RestClient getClient() {
        if (restClient == null) {
            String host = esHosts.contains("://") ? esHosts : "http://" + esHosts;
            restClient = RestClient.builder(org.apache.http.HttpHost.create(host)).build();
        }
        return restClient;
    }

    public void sync(InspireMain main) {
        if (!isEnabled()) return;
        try {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", main.getId());
            doc.put("title", main.getTitle());
            doc.put("img", main.getImg());
            doc.put("tag", main.getTag());
            doc.put("user_id", main.getUserId());
            doc.put("view_count", main.getViewCount());
            doc.put("like_count", main.getLikeCount());
            doc.put("collect_count", main.getCollectCount());
            doc.put("heat", main.getHeat());
            doc.put("publish_city", main.getPublishCity());
            doc.put("create_time", main.getCreateTime() != null ? main.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            doc.put("deleted", main.getDeleted());

            Request req = new Request("PUT", "/" + INDEX + "/_doc/" + main.getId());
            req.setJsonEntity(objectMapper.writeValueAsString(doc));
            getClient().performRequest(req);
            log.debug("ES同步成功: id={}", main.getId());
        } catch (Exception e) {
            log.warn("ES同步失败，不影响主流程: id={}, {}", main.getId(), e.getMessage());
        }
    }

    public void delete(Long id) {
        if (!isEnabled()) return;
        try {
            Request req = new Request("DELETE", "/" + INDEX + "/_doc/" + id);
            getClient().performRequest(req);
            log.debug("ES删除成功: id={}", id);
        } catch (Exception e) {
            log.warn("ES删除失败，不影响主流程: id={}, {}", id, e.getMessage());
        }
    }
}
