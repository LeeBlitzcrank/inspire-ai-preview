package com.inspire.platform.core.service.es;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.core.entity.InspireMain;
import com.inspire.platform.core.mapper.InspireMainMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class EsSyncService {

    private RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String esHosts;
    private final InspireMainMapper mainMapper;
    private final MeterRegistry meterRegistry;
    private final Timer singleWriteTimer;
    private final Timer bulkWriteTimer;
    private final Counter writeErrors;
    private static final String INDEX = "inspire_index";
    private volatile LocalDateTime lastIncrementalSync;
    private final AtomicBoolean refreshPending = new AtomicBoolean(false);

    public EsSyncService(ObjectMapper objectMapper,
                         @Value("${inspire.es.hosts:}") String esHosts,
                         InspireMainMapper mainMapper,
                         MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.esHosts = esHosts;
        this.mainMapper = mainMapper;
        this.meterRegistry = meterRegistry;
        this.singleWriteTimer = Timer.builder("inspire.es.write.latency")
                .tag("operation", "single")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.bulkWriteTimer = Timer.builder("inspire.es.write.latency")
                .tag("operation", "bulk")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.writeErrors = Counter.builder("inspire.es.write.errors")
                .register(meterRegistry);
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
        if (!isEnabled()) {
            return;
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Map<String, Object> doc = buildDoc(main);
            Request req = new Request("PUT", "/" + INDEX + "/_doc/" + main.getId());
            req.setJsonEntity(objectMapper.writeValueAsString(doc));
            getClient().performRequest(req);
            refreshPending.set(true);
            log.debug("ES同步成功: id={}", main.getId());
        } catch (Exception e) {
            writeErrors.increment();
            log.warn("ES同步失败，不影响主流程: id={}, {}", main.getId(), e.getMessage());
        } finally {
            sample.stop(singleWriteTimer);
        }
    }

    public void delete(Long id) {
        if (!isEnabled()) {
            return;
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Request req = new Request("DELETE", "/" + INDEX + "/_doc/" + id);
            getClient().performRequest(req);
            refreshPending.set(true);
            log.debug("ES删除成功: id={}", id);
        } catch (Exception e) {
            writeErrors.increment();
            log.warn("ES删除失败，不影响主流程: id={}, {}", id, e.getMessage());
        } finally {
            sample.stop(singleWriteTimer);
        }
    }

    public void batchSync() {
        if (!isEnabled() || mainMapper == null) {
            return;
        }
        try {
            List<InspireMain> mains = mainMapper.selectList(
                Wrappers.lambdaQuery(InspireMain.class)
                    .eq(InspireMain::getDeleted, 0));
            if (mains.isEmpty()) {
                return;
            }
            bulkWrite(mains);
            log.info("批量同步完成: {} 条", mains.size());
        } catch (Exception e) {
            log.warn("批量同步失败，不影响主流程: {}", e.getMessage());
        }
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 60000)
    public void syncIncremental() {
        if (!isEnabled() || mainMapper == null) {
            return;
        }
        if (lastIncrementalSync == null) {
            batchSync();
            lastIncrementalSync = LocalDateTime.now();
            return;
        }
        LocalDateTime syncStartedAt = LocalDateTime.now();
        try {
            List<InspireMain> mains = mainMapper.selectList(
                Wrappers.lambdaQuery(InspireMain.class)
                    .gt(InspireMain::getUpdateTime, lastIncrementalSync));
            if (mains.isEmpty()) {
                lastIncrementalSync = syncStartedAt;
                return;
            }
            bulkWrite(mains);
            lastIncrementalSync = syncStartedAt;
            log.info("增量同步完成: {} 条", mains.size());
        } catch (Exception e) {
            log.warn("增量同步失败，下次定时任务继续重试: {}", e.getMessage());
        }
    }

    private void bulkWrite(List<InspireMain> mains) throws Exception {
        Timer.Sample sample = Timer.start(meterRegistry);
        StringBuilder bulkBody = new StringBuilder();
        int indexed = 0, deleted = 0;
        for (InspireMain main : mains) {
            if (Integer.valueOf(1).equals(main.getDeleted())) {
                bulkBody.append("{\"delete\":{\"_index\":\"")
                    .append(INDEX)
                    .append("\",\"_id\":\"")
                    .append(main.getId())
                    .append("\"}}\n");
                deleted++;
                continue;
            }
            Map<String, Object> doc = buildDoc(main);
            bulkBody.append("{\"index\":{\"_index\":\"")
                .append(INDEX)
                .append("\",\"_id\":\"")
                .append(main.getId())
                .append("\"}}\n");
            bulkBody.append(objectMapper.writeValueAsString(doc)).append("\n");
            indexed++;
        }
        try {
            Request req = new Request("POST", "/_bulk");
            req.setJsonEntity(bulkBody.toString());
            org.elasticsearch.client.Response resp = getClient().performRequest(req);
            String respBody = org.apache.http.util.EntityUtils.toString(resp.getEntity());
            if (respBody.contains("\"errors\":true")) {
                throw new IllegalStateException("批量写入存在错误");
            }
            log.debug("ES批量写入: indexed={}, deleted={}", indexed, deleted);
            refreshPending.set(true);
        } catch (Exception e) {
            writeErrors.increment();
            throw e;
        } finally {
            sample.stop(bulkWriteTimer);
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void refreshIndex() {
        if (!isEnabled() || !refreshPending.compareAndSet(true, false)) {
            return;
        }
        try {
            getClient().performRequest(new Request("POST", "/" + INDEX + "/_refresh"));
        } catch (Exception e) {
            refreshPending.set(true);
            log.debug("ES refresh失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildDoc(InspireMain main) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", main.getId());
        doc.put("title", main.getTitle());
        doc.put("img", main.getImg());
        doc.put("tag", main.getTag());
        doc.put("status", main.getStatus());
        doc.put("category_id", main.getCategoryId());
        doc.put("sub_category_id", main.getSubCategoryId());
        doc.put("user_id", main.getUserId());
        doc.put("view_count", main.getViewCount());
        doc.put("like_count", main.getLikeCount());
        doc.put("collect_count", main.getCollectCount());
        doc.put("heat", main.getHeat());
        doc.put("publish_city", main.getPublishCity());
        doc.put("create_time", main.getCreateTime() != null
            ? main.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        doc.put("deleted", main.getDeleted());
        return doc;
    }
}
