package com.inspire.platform.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.ChunkDocument;
import com.inspire.platform.rag.model.RagModels.EmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class RagIndexService {

    private static final int MAX_IMAGE_CHUNKS = 3;
    private static final int TEXT_CHUNK_SIZE = 700;
    private static final int TEXT_OVERLAP = 80;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;
    private final EmbeddingService embeddingService;
    private final MultimodalService multimodalService;
    private final ElasticsearchRagStore store;
    private final AtomicBoolean syncing = new AtomicBoolean(false);
    private final AtomicReference<ReindexState> reindexState =
            new AtomicReference<>(ReindexState.idle());

    public RagIndexService(JdbcTemplate jdbcTemplate,
                           ObjectMapper objectMapper,
                           RagProperties properties,
                           EmbeddingService embeddingService,
                           MultimodalService multimodalService,
                           ElasticsearchRagStore store) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.multimodalService = multimodalService;
        this.store = store;
    }

    @Scheduled(initialDelay = 8_000, fixedDelayString = "${inspire.rag.sync-delay-ms:30000}")
    public void syncPending() {
        if (!properties.isEnabled() || !syncing.compareAndSet(false, true)) return;
        try {
            if (store.count() == 0 && indexedCount() > 0) {
                jdbcTemplate.update("""
                        UPDATE rag_index_state
                        SET content_hash = NULL, indexed_at = NULL
                        """);
                log.warn("检测到 RAG ES 索引为空，已标记状态并触发重新索引");
            }
            List<Long> ids = jdbcTemplate.queryForList("""
                    SELECT m.id
                    FROM inspire_main m
                    LEFT JOIN rag_index_state s ON s.inspire_id = m.id
                    WHERE m.deleted = 0 AND m.status = 1
                      AND (s.inspire_id IS NULL OR s.content_hash IS NULL
                           OR s.source_update_time IS NULL OR m.update_time > s.source_update_time)
                    ORDER BY m.update_time ASC, m.id ASC
                    LIMIT ?
                    """, Long.class, properties.getSyncBatchSize());
            for (Long id : ids) {
                indexInspire(id);
            }
            if (!ids.isEmpty()) {
                log.info("RAG 增量同步完成: batch={}, indexedTotal={}", ids.size(), indexedCount());
            }
        } catch (Exception e) {
            log.warn("RAG 增量同步失败: {}", e.getMessage());
        } finally {
            syncing.set(false);
        }
    }

    public int indexAll(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 5000));
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM inspire_main
                WHERE deleted = 0 AND status = 1
                ORDER BY update_time DESC, id DESC
                LIMIT ?
                """, Long.class, safeLimit);
        long startedAt = System.currentTimeMillis();
        reindexState.set(ReindexState.waiting(ids.size(), startedAt));
        if (!acquireSyncLock(Duration.ofMinutes(5))) {
            IllegalStateException error =
                    new IllegalStateException("等待 RAG 增量索引结束超时，请稍后重试");
            reindexState.updateAndGet(state -> state.failed(error.getMessage()));
            throw error;
        }
        try {
            reindexState.set(ReindexState.started(ids.size(), startedAt));
            int indexed = indexBatched(ids, startedAt);
            reindexState.updateAndGet(state -> state.completed(indexed));
            return indexed;
        } catch (Exception e) {
            reindexState.updateAndGet(state -> state.failed(e.getMessage()));
            throw e;
        } finally {
            syncing.set(false);
        }
    }

    private boolean acquireSyncLock(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (syncing.compareAndSet(false, true)) return true;
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public ReindexState reindexState() {
        return reindexState.get();
    }

    public boolean indexInspire(long inspireId) {
        return indexInspire(inspireId, false);
    }

    public boolean indexInspire(long inspireId, boolean force) {
        SourceRow row = loadSource(inspireId);
        if (row == null) {
            deleteInspire(inspireId);
            return false;
        }

        String hash = sha256(row.title() + "\n" + row.content() + "\n" + row.images() + "\n"
                + row.tag() + "\n" + row.seriesName() + "\n" + row.updateTime());
        String previous = jdbcTemplate.query(
                "SELECT content_hash FROM rag_index_state WHERE inspire_id = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                inspireId
        );
        if (!force && Objects.equals(hash, previous)) return false;

        try {
            List<ChunkDocument> chunks = buildChunks(row);
            store.replaceDocument(inspireId, chunks);
            saveIndexedState(row, hash, chunks.size());
            return true;
        } catch (Exception e) {
            saveIndexError(row, e);
            log.warn("RAG 索引失败 inspireId={}: {}", inspireId, e.getMessage());
            return false;
        }
    }

    public void deleteInspire(long inspireId) {
        store.deleteDocument(inspireId);
        jdbcTemplate.update("DELETE FROM rag_index_state WHERE inspire_id = ?", inspireId);
    }

    public long indexedCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rag_index_state WHERE indexed_at IS NOT NULL",
                Long.class
        );
        return count == null ? 0 : count;
    }

    public long publishedCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE deleted = 0 AND status = 1",
                Long.class
        );
        return count == null ? 0 : count;
    }

    private List<ChunkDocument> buildChunks(SourceRow row) {
        List<PendingChunk> pending = buildPendingChunks(row);
        List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                pending.stream().map(PendingChunk::embeddingText).toList()
        );
        return toChunkDocuments(pending, embeddings);
    }

    private List<PendingChunk> buildPendingChunks(SourceRow row) {
        List<PendingChunk> pending = new ArrayList<>();
        String commonHeader = "标题：" + row.title() + "\n"
                + "分类：" + safe(row.tag()) + "\n"
                + "城市：" + safe(row.city()) + "\n"
                + "系列：" + safe(row.seriesName()) + "\n";
        List<String> textChunks = splitText(row.content(), TEXT_CHUNK_SIZE, TEXT_OVERLAP);
        if (textChunks.isEmpty()) textChunks = List.of("");
        for (int i = 0; i < textChunks.size(); i++) {
            String content = commonHeader + "正文：" + textChunks.get(i);
            pending.add(new PendingChunk(
                    row.id(),
                    row.id() + ":text:" + i,
                    "text",
                    row.title(),
                    textChunks.get(i),
                    null,
                    row.tag(),
                    row.city(),
                    row.authorId(),
                    row.seriesId(),
                    row.imageUrls().isEmpty() ? "" : row.imageUrls().get(0),
                    content
            ));
        }

        for (int i = 0; i < row.imageUrls().size() && i < MAX_IMAGE_CHUNKS; i++) {
            String imageUrl = row.imageUrls().get(i);
            String fallback = row.title() + "；分类：" + safe(row.tag());
            String caption = multimodalService.describeImageUrl(imageUrl, fallback);
            String content = commonHeader + "图片描述：" + caption;
            pending.add(new PendingChunk(
                    row.id(),
                    row.id() + ":image:" + i,
                    "image",
                    row.title(),
                    "",
                    caption,
                    row.tag(),
                    row.city(),
                    row.authorId(),
                    row.seriesId(),
                    imageUrl,
                    content
            ));
        }
        return pending;
    }

    private List<ChunkDocument> toChunkDocuments(List<PendingChunk> pending, List<EmbeddingResult> embeddings) {
        List<ChunkDocument> chunks = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            PendingChunk item = pending.get(i);
            chunks.add(new ChunkDocument(
                    item.inspireId(),
                    item.chunkId(),
                    item.modality(),
                    item.title(),
                    item.content(),
                    item.caption(),
                    item.tag(),
                    item.city(),
                    item.authorId(),
                    item.seriesId(),
                    item.imageUrl(),
                    embeddings.get(i).vector()
            ));
        }
        return chunks;
    }

    private int indexBatched(List<Long> ids, long startedAt) {
        List<DocumentWork> works = new ArrayList<>();
        List<PendingChunk> allPending = new ArrayList<>();
        for (Long id : ids) {
            SourceRow row = loadSource(id);
            if (row == null) {
                deleteInspire(id);
                continue;
            }
            List<PendingChunk> pending = buildPendingChunks(row);
            works.add(new DocumentWork(row, pending));
            allPending.addAll(pending);
        }
        if (allPending.isEmpty()) return 0;
        reindexState.updateAndGet(state ->
                state.embedding(works.size(), allPending.size(), startedAt));

        List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                allPending.stream().map(PendingChunk::embeddingText).toList(),
                completed -> reindexState.updateAndGet(state -> state.chunkProgress(completed))
        );
        reindexState.updateAndGet(ReindexState::writing);
        int cursor = 0;
        int indexed = 0;
        for (DocumentWork work : works) {
            int count = work.pending().size();
            try {
                List<EmbeddingResult> slice = embeddings.subList(cursor, cursor + count);
                List<ChunkDocument> chunks = toChunkDocuments(work.pending(), slice);
                store.replaceDocument(work.row().id(), chunks);
                saveIndexedState(work.row(), contentHash(work.row()), chunks.size());
                indexed++;
            } catch (Exception e) {
                saveIndexError(work.row(), e);
                log.warn("RAG 批量索引失败 inspireId={}: {}", work.row().id(), e.getMessage());
            } finally {
                cursor += count;
                reindexState.updateAndGet(state ->
                        state.documentProgress(Math.min(works.size(), state.processedDocuments() + 1)));
            }
        }
        return indexed;
    }

    private String contentHash(SourceRow row) {
        return sha256(row.title() + "\n" + row.content() + "\n" + row.images() + "\n"
                + row.tag() + "\n" + row.seriesName() + "\n" + row.updateTime());
    }

    private void saveIndexedState(SourceRow row, String hash, int chunkCount) {
        jdbcTemplate.update("""
                INSERT INTO rag_index_state
                    (inspire_id, content_hash, chunk_count, source_update_time, indexed_at, error_message)
                VALUES (?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    content_hash = VALUES(content_hash),
                    chunk_count = VALUES(chunk_count),
                    source_update_time = VALUES(source_update_time),
                    indexed_at = VALUES(indexed_at),
                    error_message = NULL
                """,
                row.id(),
                hash,
                chunkCount,
                row.updateTime(),
                LocalDateTime.now()
        );
    }

    private void saveIndexError(SourceRow row, Exception exception) {
        jdbcTemplate.update("""
                INSERT INTO rag_index_state
                    (inspire_id, content_hash, chunk_count, source_update_time, indexed_at, error_message)
                VALUES (?, NULL, 0, ?, NULL, ?)
                ON DUPLICATE KEY UPDATE error_message = VALUES(error_message)
                """,
                row.id(),
                row.updateTime(),
                truncate(exception.getMessage(), 480)
        );
    }

    private SourceRow loadSource(long inspireId) {
        return jdbcTemplate.query("""
                SELECT m.id, m.title, m.img, m.images, m.tag, m.publish_city, m.user_id,
                       m.series_id, m.update_time, c.content, s.name AS series_name
                FROM inspire_main m
                LEFT JOIN inspire_content c ON c.inspire_id = m.id
                LEFT JOIN inspire_series s ON s.id = m.series_id AND s.deleted = 0
                WHERE m.id = ? AND m.deleted = 0 AND m.status = 1
                """, rs -> {
            if (!rs.next()) return null;
            List<String> images = parseImages(rs.getString("images"));
            String cover = Objects.toString(rs.getString("img"), "");
            if (!cover.isBlank()) images.add(0, cover);
            Set<String> dedup = new LinkedHashSet<>(images);
            return new SourceRow(
                    rs.getLong("id"),
                    Objects.toString(rs.getString("title"), ""),
                    Objects.toString(rs.getString("content"), ""),
                    Objects.toString(rs.getString("images"), ""),
                    Objects.toString(rs.getString("tag"), ""),
                    Objects.toString(rs.getString("publish_city"), ""),
                    rs.getLong("user_id"),
                    rs.getObject("series_id") == null ? null : rs.getLong("series_id"),
                    Objects.toString(rs.getString("series_name"), ""),
                    rs.getTimestamp("update_time"),
                    new ArrayList<>(dedup)
            );
        }, inspireId);
    }

    private List<String> parseImages(String images) {
        if (!StringUtils.hasText(images)) return new ArrayList<>();
        try {
            return new ArrayList<>(objectMapper.readValue(images, new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> splitText(String text, int size, int overlap) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return List.of();
        if (value.length() <= size) return List.of(value);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int end = Math.min(value.length(), start + size);
            chunks.add(value.substring(start, end));
            if (end >= value.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record SourceRow(
            long id,
            String title,
            String content,
            String images,
            String tag,
            String city,
            long authorId,
            Long seriesId,
            String seriesName,
            Timestamp updateTime,
            List<String> imageUrls
    ) {
    }

    private record PendingChunk(
            long inspireId,
            String chunkId,
            String modality,
            String title,
            String content,
            String caption,
            String tag,
            String city,
            long authorId,
            Long seriesId,
            String imageUrl,
            String embeddingText
    ) {
    }

    private record DocumentWork(SourceRow row, List<PendingChunk> pending) {
    }

    public record ReindexState(
            boolean running,
            String phase,
            int totalDocuments,
            int processedDocuments,
            int totalChunks,
            int processedChunks,
            long startedAt,
            long finishedAt,
            String errorMessage
    ) {
        static ReindexState idle() {
            return new ReindexState(false, "idle", 0, 0, 0, 0, 0, 0, null);
        }

        static ReindexState waiting(int totalDocuments, long startedAt) {
            return new ReindexState(true, "waiting", totalDocuments, 0, 0, 0, startedAt, 0, null);
        }

        static ReindexState started(int totalDocuments, long startedAt) {
            return new ReindexState(true, "loading", totalDocuments, 0, 0, 0, startedAt, 0, null);
        }

        ReindexState embedding(int totalDocuments, int totalChunks, long startedAt) {
            return new ReindexState(true, "embedding", totalDocuments, 0,
                    totalChunks, 0, startedAt, 0, null);
        }

        ReindexState chunkProgress(int processedChunks) {
            return new ReindexState(true, "embedding", totalDocuments, processedDocuments,
                    totalChunks, Math.min(totalChunks, processedChunks), startedAt, 0, null);
        }

        ReindexState writing() {
            return new ReindexState(true, "writing", totalDocuments, processedDocuments,
                    totalChunks, totalChunks, startedAt, 0, null);
        }

        ReindexState documentProgress(int processedDocuments) {
            return new ReindexState(true, "writing", totalDocuments,
                    Math.min(totalDocuments, processedDocuments), totalChunks, totalChunks,
                    startedAt, 0, null);
        }

        ReindexState completed(int indexedDocuments) {
            return new ReindexState(false, "completed", totalDocuments,
                    Math.max(processedDocuments, indexedDocuments), totalChunks, totalChunks,
                    startedAt, System.currentTimeMillis(), null);
        }

        ReindexState failed(String errorMessage) {
            return new ReindexState(false, "failed", totalDocuments, processedDocuments,
                    totalChunks, processedChunks, startedAt, System.currentTimeMillis(), errorMessage);
        }
    }
}
