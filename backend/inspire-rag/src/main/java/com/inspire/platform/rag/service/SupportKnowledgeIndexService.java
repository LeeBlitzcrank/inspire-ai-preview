package com.inspire.platform.rag.service;

import com.inspire.platform.rag.config.SupportProperties;
import com.inspire.platform.rag.model.RagModels.EmbeddingResult;
import com.inspire.platform.rag.model.SupportModels.SupportChunkDocument;
import com.inspire.platform.rag.model.SupportModels.SupportIndexState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportKnowledgeIndexService {

    private static final int STATE_ID = 1;
    private static final String INDEX_FORMAT_VERSION = "support-v6";

    private final SupportProperties properties;
    private final EmbeddingService embeddingService;
    private final SupportKnowledgeStore store;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SupportIndexState> state =
            new AtomicReference<>(new SupportIndexState(false, "idle", 0, 0, null, null, null));

    @Scheduled(initialDelay = 12_000, fixedDelayString = "${inspire.support-rag.scan-delay-ms:600000}")
    public void syncIfChanged() {
        if (!properties.isEnabled() || running.get()) return;
        try {
            SourceSnapshot snapshot = scan();
            StateRow previous = loadState();
            if (previous != null && snapshot.fingerprint().equals(previous.fingerprint())
                    && previous.indexedAt() != null && store.count() > 0) {
                state.set(toState(false, "ready", previous, snapshot.fingerprint()));
                return;
            }
            reindex();
        } catch (Exception e) {
            log.warn("客服知识库自动同步失败: {}", e.getMessage());
        }
    }

    public SupportIndexState reindex() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("客服知识库未启用");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("客服知识库正在重建，请稍后");
        }
        long started = System.currentTimeMillis();
        try {
            state.set(new SupportIndexState(true, "scanning", 0, 0, null, null, null));
            SourceSnapshot snapshot = scan();
            if (snapshot.files().isEmpty()) {
                throw new IllegalStateException("未找到可索引的项目文档");
            }
            List<PendingChunk> pending = new ArrayList<>();
            for (SourceFile file : snapshot.files()) {
                pending.addAll(buildChunks(file));
            }
            state.set(new SupportIndexState(true, "embedding",
                    snapshot.files().size(), pending.size(), snapshot.fingerprint(), null, null));

            List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                    pending.stream().map(PendingChunk::embeddingText).toList());
            List<SupportChunkDocument> documents = new ArrayList<>(pending.size());
            for (int i = 0; i < pending.size(); i++) {
                PendingChunk chunk = pending.get(i);
                documents.add(new SupportChunkDocument(
                        chunk.chunkId(),
                        chunk.docId(),
                        chunk.title(),
                        chunk.section(),
                        chunk.content(),
                        chunk.filePath(),
                        chunk.updatedAt(),
                        embeddings.get(i).vector()
                ));
            }
            state.set(new SupportIndexState(true, "writing",
                    snapshot.files().size(), documents.size(), snapshot.fingerprint(), null, null));
            store.replaceAll(documents);
            LocalDateTime indexedAt = LocalDateTime.now();
            jdbcTemplate.update("""
                    INSERT INTO support_rag_state
                        (id,fingerprint,document_count,chunk_count,status,error_message,indexed_at,update_time)
                    VALUES (1,?,?,?, 'ready', NULL, ?, NOW())
                    ON DUPLICATE KEY UPDATE
                        fingerprint=VALUES(fingerprint),
                        document_count=VALUES(document_count),
                        chunk_count=VALUES(chunk_count),
                        status='ready',
                        error_message=NULL,
                        indexed_at=VALUES(indexed_at),
                        update_time=NOW()
                    """, snapshot.fingerprint(), snapshot.files().size(), documents.size(), indexedAt);
            SupportIndexState completed = new SupportIndexState(false, "ready",
                    snapshot.files().size(), documents.size(), snapshot.fingerprint(),
                    null, indexedAt.toString());
            state.set(completed);
            log.info("客服知识库重建完成: files={}, chunks={}, tookMs={}",
                    snapshot.files().size(), documents.size(), System.currentTimeMillis() - started);
            return completed;
        } catch (Exception e) {
            String message = abbreviate(e.getMessage(), 480);
            jdbcTemplate.update("""
                    INSERT INTO support_rag_state
                        (id,status,error_message,update_time)
                    VALUES (1,'failed',?,NOW())
                    ON DUPLICATE KEY UPDATE
                        status='failed', error_message=VALUES(error_message), update_time=NOW()
            """, message);
            state.set(new SupportIndexState(false, "failed", 0, 0, null, message, null));
            throw new IllegalStateException(message, e);
        } finally {
            running.set(false);
        }
    }

    public SupportIndexState state() {
        SupportIndexState current = state.get();
        if (current.running()) return current;
        StateRow row = loadState();
        if (row == null) return state.get();
        return toState(false, row.status(), row, row.fingerprint());
    }

    public long count() {
        return store.count();
    }

    private SourceSnapshot scan() throws Exception {
        Path root = Paths.get(properties.getRootDir()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("客服知识库目录不存在: " + root);
        }
        List<SourceFile> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> included(root, path))
                    .sorted()
                    .map(path -> toSourceFile(root, path))
                    .toList();
        }
        StringBuilder fingerprintInput = new StringBuilder(INDEX_FORMAT_VERSION).append('\n');
        for (SourceFile file : files) {
            fingerprintInput.append(file.relativePath()).append(':')
                    .append(file.size()).append(':')
                    .append(file.modifiedAtEpochMs()).append('\n');
        }
        return new SourceSnapshot(files, sha256(fingerprintInput.toString()));
    }

    private boolean included(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String lower = relative.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".md")) return false;
        String configured = properties.getKnowledgeFile()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);
        return lower.equals(configured);
    }

    private SourceFile toSourceFile(Path root, Path path) {
        try {
            String relative = root.relativize(path).toString().replace('\\', '/');
            FileTime modified = Files.getLastModifiedTime(path);
            return new SourceFile(
                    path,
                    relative,
                    Files.size(path),
                    modified.toMillis(),
                    Files.readString(path, StandardCharsets.UTF_8),
                    Instant.ofEpochMilli(modified.toMillis()).toString()
            );
        } catch (Exception e) {
            throw new IllegalStateException("读取文档失败: " + path, e);
        }
    }

    private List<PendingChunk> buildChunks(SourceFile file) {
        if (file.size() > properties.getMaxFileBytes()) {
            log.warn("跳过过大的客服文档: file={}, size={}", file.relativePath(), file.size());
            return List.of();
        }
        List<SectionText> sections = splitSections(file.content());
        List<PendingChunk> chunks = new ArrayList<>();
        String docId = sha256(file.relativePath());
        String title = fileTitle(file.content(), file.relativePath());
        for (SectionText section : sections) {
            List<String> pieces = splitText(section.content(), properties.getChunkSize(),
                    properties.getChunkOverlap());
            for (int i = 0; i < pieces.size(); i++) {
                String content = pieces.get(i).trim();
                if (content.isEmpty()) continue;
                String chunkId = docId + ":" + chunks.size();
                String embeddingText = "文档：" + title + "\n"
                        + "章节：" + section.section() + "\n"
                        + "文件：" + file.relativePath() + "\n\n"
                        + content;
                chunks.add(new PendingChunk(
                        chunkId,
                        docId,
                        title,
                        section.section(),
                        content,
                        file.relativePath(),
                        file.updatedAt(),
                        embeddingText
                ));
            }
        }
        return chunks;
    }

    private List<SectionText> splitSections(String markdown) {
        List<SectionText> sections = new ArrayList<>();
        List<String> headingStack = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : markdown.split("\\R", -1)) {
            int level = headingLevel(line);
            if (level > 0) {
                flushSection(sections, headingStack, current);
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(line.substring(level).trim());
                continue;
            }
            current.append(line).append('\n');
        }
        flushSection(sections, headingStack, current);
        return sections;
    }

    private void flushSection(List<SectionText> sections,
                              List<String> headingStack,
                              StringBuilder current) {
        String content = current.toString().trim();
        current.setLength(0);
        if (content.isEmpty()) return;
        String section = headingStack.isEmpty()
                ? "正文"
                : String.join(" / ", headingStack);
        sections.add(new SectionText(section, content));
    }

    private int headingLevel(String line) {
        int count = 0;
        while (count < line.length() && count < 6 && line.charAt(count) == '#') {
            count++;
        }
        return count > 0 && count < line.length() && line.charAt(count) == ' ' ? count : 0;
    }

    private List<String> splitText(String text, int size, int overlap) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) return List.of();
        if (normalized.length() <= size) return List.of(normalized);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + Math.max(100, size));
            if (end < normalized.length()) {
                int paragraph = normalized.lastIndexOf("\n\n", end);
                int sentence = normalized.lastIndexOf('。', end);
                int preferred = Math.max(paragraph, sentence);
                if (preferred > start + size / 2) end = preferred + 1;
            }
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) break;
            start = Math.max(start + 1, end - Math.max(0, overlap));
        }
        return chunks;
    }

    private String fileTitle(String content, String filePath) {
        for (String line : content.split("\\R")) {
            if (line.startsWith("# ")) return line.substring(2).trim();
        }
        return filePath;
    }

    private StateRow loadState() {
        try {
            return jdbcTemplate.query("""
                    SELECT fingerprint,document_count,chunk_count,status,error_message,indexed_at
                    FROM support_rag_state WHERE id = 1
                    """, rs -> rs.next()
                    ? new StateRow(
                            rs.getString("fingerprint"),
                            rs.getInt("document_count"),
                            rs.getInt("chunk_count"),
                            rs.getString("status"),
                            rs.getString("error_message"),
                            rs.getTimestamp("indexed_at") == null
                                    ? null : rs.getTimestamp("indexed_at").toLocalDateTime().toString())
                    : null);
        } catch (Exception e) {
            return null;
        }
    }

    private SupportIndexState toState(boolean isRunning, String status, StateRow row, String fingerprint) {
        return new SupportIndexState(
                isRunning,
                status,
                row.documentCount(),
                row.chunkCount(),
                fingerprint,
                row.errorMessage(),
                row.indexedAt()
        );
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("客服知识库哈希失败", e);
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) return "未知错误";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record SourceFile(
            Path path,
            String relativePath,
            long size,
            long modifiedAtEpochMs,
            String content,
            String updatedAt
    ) {
    }

    private record SourceSnapshot(List<SourceFile> files, String fingerprint) {
    }

    private record SectionText(String section, String content) {
    }

    private record PendingChunk(
            String chunkId,
            String docId,
            String title,
            String section,
            String content,
            String filePath,
            String updatedAt,
            String embeddingText
    ) {
    }

    private record StateRow(
            String fingerprint,
            int documentCount,
            int chunkCount,
            String status,
            String errorMessage,
            String indexedAt
    ) {
    }
}
