/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/service/RecommendationVectorIndexService.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：为推荐系统维护每篇灵感一条向量文档
 * 维护说明：推荐向量只保存内容语义，不保存用户隐私或推荐行为。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.EmbeddingResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationVectorIndexService {

    private final RagProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean syncing = new AtomicBoolean(false);
    private RestClient client;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) return;
        String host = properties.getElasticsearchHost();
        if (!host.contains("://")) host = "http://" + host;
        client = RestClient.builder(HttpHost.create(host)).build();
        ensureIndex();
    }

    @Scheduled(initialDelay = 12_000, fixedDelayString = "${inspire.rag.sync-delay-ms:30000}")
    public void syncPending() {
        if (!properties.isEnabled() || !syncing.compareAndSet(false, true)) return;
        try {
            List<Long> ids = jdbcTemplate.queryForList("""
                    SELECT m.id
                    FROM inspire_main m
                    LEFT JOIN recommend_vector_state s ON s.inspire_id = m.id
                    WHERE m.deleted = 0 AND m.status = 1
                      AND (s.inspire_id IS NULL OR s.content_hash IS NULL
                           OR s.source_update_time IS NULL OR m.update_time > s.source_update_time)
                    ORDER BY m.update_time ASC, m.id ASC
                    LIMIT ?
                    """, Long.class, properties.getRecommendVectorSyncBatchSize());
            if (ids.isEmpty()) return;
            List<SourceRow> rows = new ArrayList<>();
            for (Long id : ids) {
                SourceRow row = loadSource(id);
                if (row != null) rows.add(row);
            }
            if (rows.isEmpty()) return;
            List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                    rows.stream().map(this::embeddingText).toList());
            for (int i = 0; i < rows.size(); i++) {
                writeVector(rows.get(i), embeddings.get(i).vector());
            }
            log.info("推荐向量增量同步完成: {}", rows.size());
        } catch (Exception e) {
            log.warn("推荐向量增量同步失败: {}", e.getMessage());
        } finally {
            syncing.set(false);
        }
    }

    public int reindex(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 5000));
        jdbcTemplate.update("""
                UPDATE recommend_vector_state
                SET content_hash = NULL, indexed_at = NULL
                """);
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM inspire_main
                WHERE deleted = 0 AND status = 1
                ORDER BY update_time DESC, id DESC
                LIMIT ?
                """, Long.class, safeLimit);
        List<SourceRow> rows = new ArrayList<>();
        for (Long id : ids) {
            SourceRow row = loadSource(id);
            if (row != null) rows.add(row);
        }
        if (rows.isEmpty()) return 0;
        List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                rows.stream().map(this::embeddingText).toList());
        for (int i = 0; i < rows.size(); i++) {
            writeVector(rows.get(i), embeddings.get(i).vector());
        }
        return rows.size();
    }

    public long indexedCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommend_vector_state WHERE indexed_at IS NOT NULL",
                Long.class);
        return count == null ? 0 : count;
    }

    public long publishedCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE deleted = 0 AND status = 1",
                Long.class);
        return count == null ? 0 : count;
    }

    private SourceRow loadSource(Long id) {
        List<SourceRow> rows = jdbcTemplate.query("""
                SELECT m.id,m.title,m.tag,m.user_id,m.heat,m.create_time,m.update_time,
                       COALESCE(c.content,'') AS content
                FROM inspire_main m
                LEFT JOIN inspire_content c ON c.inspire_id = m.id
                WHERE m.id = ? AND m.deleted = 0 AND m.status = 1
                """, (rs, rowNum) -> new SourceRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("tag"),
                rs.getLong("user_id"),
                rs.getInt("heat"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        ), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String embeddingText(SourceRow row) {
        String content = row.content() == null ? "" : row.content();
        if (content.length() > 2400) content = content.substring(0, 2400);
        return "标题：" + safe(row.title()) + "\n"
                + "分类：" + safe(row.tag()) + "\n"
                + "正文：" + content;
    }

    private void writeVector(SourceRow row, float[] vector) {
        String hash = sha256(embeddingText(row) + row.updateTime());
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("inspire_id", row.id());
        doc.put("title", row.title());
        doc.put("tag", row.tag());
        doc.put("author_id", row.authorId());
        doc.put("heat", row.heat());
        doc.put("create_time", row.createTime().toString());
        doc.put("update_time", row.updateTime().toString());
        doc.put("embedding", toList(vector));
        try {
            Request request = new Request("PUT",
                    "/" + properties.getRecommendVectorIndex() + "/_doc/" + row.id() + "?refresh=false");
            request.setEntity(new NStringEntity(
                    objectMapper.writeValueAsString(doc), ContentType.APPLICATION_JSON));
            client.performRequest(request);
            jdbcTemplate.update("""
                    INSERT INTO recommend_vector_state
                        (inspire_id,content_hash,source_update_time,indexed_at,error_message)
                    VALUES (?,?,?,?,NULL)
                    ON DUPLICATE KEY UPDATE
                        content_hash=VALUES(content_hash),
                        source_update_time=VALUES(source_update_time),
                        indexed_at=VALUES(indexed_at),
                        error_message=NULL
                    """, row.id(), hash, row.updateTime(), LocalDateTime.now());
        } catch (Exception e) {
            jdbcTemplate.update("""
                    INSERT INTO recommend_vector_state
                        (inspire_id,error_message)
                    VALUES (?,?)
                    ON DUPLICATE KEY UPDATE error_message=VALUES(error_message)
                    """, row.id(), abbreviate(e.getMessage(), 500));
            log.warn("推荐向量写入失败 inspireId={}: {}", row.id(), e.getMessage());
        }
    }

    private void ensureIndex() {
        try {
            client.performRequest(new Request("HEAD", "/" + properties.getRecommendVectorIndex()));
            return;
        } catch (Exception ignored) {
            // create below
        }
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("inspire_id", Map.of("type", "long"));
            fields.put("title", Map.of("type", "text"));
            fields.put("tag", Map.of("type", "keyword"));
            fields.put("author_id", Map.of("type", "long"));
            fields.put("heat", Map.of("type", "integer"));
            fields.put("create_time", Map.of("type", "keyword"));
            fields.put("update_time", Map.of("type", "keyword"));
            fields.put("embedding", Map.of(
                    "type", "dense_vector",
                    "dims", properties.getEmbeddingDims()
            ));
            Map<String, Object> mapping = Map.of(
                    "settings", Map.of("number_of_shards", 1, "number_of_replicas", 0),
                    "mappings", Map.of("properties", fields)
            );
            Request request = new Request("PUT", "/" + properties.getRecommendVectorIndex());
            request.setEntity(new NStringEntity(
                    objectMapper.writeValueAsString(mapping), ContentType.APPLICATION_JSON));
            client.performRequest(request);
            log.info("推荐向量索引已创建: {}", properties.getRecommendVectorIndex());
        } catch (Exception e) {
            log.warn("推荐向量索引创建失败: {}", e.getMessage());
        }
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) values.add(value);
        return values;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("推荐向量哈希失败", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record SourceRow(
            long id,
            String title,
            String content,
            String tag,
            long authorId,
            int heat,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }
}
