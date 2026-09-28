package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.EmbeddingResult;
import com.inspire.platform.rag.model.RagModels.RagResponse;
import com.inspire.platform.rag.model.RagModels.RagSource;
import com.inspire.platform.rag.model.RagModels.SearchHit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagAnswerService {

    private static final int RRF_K = 60;

    private final RagProperties properties;
    private final EmbeddingService embeddingService;
    private final MultimodalService multimodalService;
    private final ElasticsearchRagStore store;
    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    public RagAnswerService(RagProperties properties,
                            EmbeddingService embeddingService,
                            MultimodalService multimodalService,
                            ElasticsearchRagStore store,
                            RestTemplate ragRestTemplate,
                            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.multimodalService = multimodalService;
        this.store = store;
        this.restTemplate = ragRestTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RagResponse search(String query, String imageBase64, Integer requestedTopK, Long userId) {
        return execute(query, imageBase64, requestedTopK, userId, false);
    }

    public RagResponse ask(String query, String imageBase64, Integer requestedTopK, Long userId) {
        return execute(query, imageBase64, requestedTopK, userId, true);
    }

    private RagResponse execute(String query, String imageBase64, Integer requestedTopK, Long userId, boolean generateAnswer) {
        long started = System.currentTimeMillis();
        String originalQuery = query == null ? "" : query.trim();
        if (!StringUtils.hasText(originalQuery) && !StringUtils.hasText(imageBase64)) {
            throw new BusinessException(400, "请输入问题或上传图片");
        }
        String imageCaption = "";
        if (StringUtils.hasText(imageBase64)) {
            imageCaption = multimodalService.describeImageBase64(imageBase64);
            if (!StringUtils.hasText(imageCaption)) {
                throw new BusinessException(400, "图片无法识别，请上传 JPG/PNG，或启用 Ollama 视觉模型");
            }
        }
        String queryText = String.join(" ", List.of(originalQuery, imageCaption).stream()
                .filter(StringUtils::hasText)
                .toList());
        int topK = requestedTopK == null ? properties.getDefaultTopK() : requestedTopK;
        topK = Math.max(1, Math.min(topK, 20));

        EmbeddingResult embedding = embeddingService.embed(queryText);
        boolean semanticQuery = embedding.provider().startsWith("ollama:");
        List<SearchHit> vectorHits = semanticQuery
                ? store.searchVector(embedding.vector(), properties.getCandidateK())
                : List.of();
        List<SearchHit> keywordHits = store.searchKeyword(originalQuery, properties.getCandidateK());
        List<RagSource> sources = fuse(vectorHits, keywordHits, topK);
        if (!semanticQuery && properties.isOllamaEmbeddingEnabled()) {
            log.warn("Ollama 查询向量不可用，本次仅使用关键词检索");
        }

        String answer = null;
        if (generateAnswer) {
            answer = buildAnswer(originalQuery, imageCaption, sources);
        }
        long took = System.currentTimeMillis() - started;
        logQuery(userId, queryText, generateAnswer ? "ask" : "search", topK, sources.size(), took);
        return new RagResponse(answer, sources, embedding.provider(), imageCaption, took);
    }

    private List<RagSource> fuse(List<SearchHit> vectorHits, List<SearchHit> keywordHits, int topK) {
        Map<Long, MutableHit> merged = new LinkedHashMap<>();
        for (int i = 0; i < vectorHits.size(); i++) {
            add(merged, vectorHits.get(i), 1.0 / (RRF_K + i + 1));
        }
        for (int i = 0; i < keywordHits.size(); i++) {
            add(merged, keywordHits.get(i), 0.85 / (RRF_K + i + 1));
        }
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(MutableHit::score).reversed())
                .limit(topK)
                .map(hit -> new RagSource(
                        hit.best().inspireId(),
                        hit.best().title(),
                        excerpt(hit.best()),
                        hit.best().tag(),
                        hit.best().imageUrl(),
                        Math.round(hit.score() * 10000d) / 10000d,
                        "/detail/" + hit.best().inspireId()
                ))
                .toList();
    }

    private void add(Map<Long, MutableHit> merged, SearchHit hit, double rrfScore) {
        MutableHit next = merged.computeIfAbsent(hit.inspireId(), id -> new MutableHit(hit));
        next.score += rrfScore;
        if (hit.score() > next.best().score()) next.best = hit;
    }

    private String excerpt(SearchHit hit) {
        String value = StringUtils.hasText(hit.caption()) ? hit.caption() : hit.content();
        if (!StringUtils.hasText(value)) value = hit.title();
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() > 110 ? value.substring(0, 110) + "…" : value;
    }

    private String buildAnswer(String query, String imageCaption, List<RagSource> sources) {
        if (sources.isEmpty()) {
            return "当前灵感库里没有找到足够相关的内容。可以换一个更具体的描述，例如加入场景、颜色、氛围或内容类型。";
        }
        if (!StringUtils.hasText(properties.getDeepseekApiKey())) {
            return fallbackAnswer(query, imageCaption, sources);
        }
        try {
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < sources.size(); i++) {
                RagSource source = sources.get(i);
                context.append("[").append(i + 1).append("] ")
                        .append(source.title()).append('\n')
                        .append("标签：").append(source.tag()).append('\n')
                        .append("摘要：").append(source.excerpt()).append('\n')
                        .append("链接：").append(source.url()).append("\n\n");
            }
            String userPrompt = "用户问题：" + query + "\n"
                    + (StringUtils.hasText(imageCaption) ? "上传图片的视觉描述：" + imageCaption + "\n" : "")
                    + "\n检索到的灵感：\n" + context
                    + "\n请只依据以上材料回答。先给一句直接结论，再用 3-6 条说明推荐理由。"
                    + "每条理由要引用对应编号，最后列出最值得先看的 2-3 条。不要编造材料中不存在的功能或内容。";
            Map<String, Object> body = new HashMap<>();
            body.put("model", properties.getDeepseekModel());
            body.put("temperature", 0.35);
            body.put("max_tokens", 900);
            body.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是灵感平台的检索助手。你只能引用给定材料，回答简洁、具体、有判断力。"),
                    Map.of("role", "user", "content", userPrompt)
            ));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(properties.getDeepseekApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            JsonNode response = restTemplate.postForObject(
                    properties.getDeepseekApiUrl(),
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            String answer = response == null
                    ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("");
            return StringUtils.hasText(answer) ? answer.trim() : fallbackAnswer(query, imageCaption, sources);
        } catch (Exception e) {
            log.warn("RAG 回答生成失败，使用检索摘要: {}", e.getMessage());
            return fallbackAnswer(query, imageCaption, sources);
        }
    }

    private String fallbackAnswer(String query, String imageCaption, List<RagSource> sources) {
        StringBuilder answer = new StringBuilder();
        answer.append("根据当前灵感库，为你找到 ").append(sources.size()).append(" 条相关内容。");
        if (StringUtils.hasText(imageCaption)) {
            answer.append("图片特征为“").append(imageCaption).append("”。");
        }
        answer.append("\n\n");
        for (int i = 0; i < sources.size(); i++) {
            RagSource source = sources.get(i);
            answer.append(i + 1).append(". 《").append(source.title()).append("》：")
                    .append(source.excerpt()).append('\n');
        }
        return answer.toString().trim();
    }

    private void logQuery(Long userId, String query, String mode, int topK, int hits, long tookMs) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO rag_query_log
                        (id, user_id, query_text, mode, top_k, hit_count, latency_ms, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                    """,
                    System.nanoTime(),
                    userId,
                    query.length() > 500 ? query.substring(0, 500) : query,
                    mode,
                    topK,
                    hits,
                    tookMs
            );
        } catch (Exception e) {
            log.debug("RAG query log 写入失败: {}", e.getMessage());
        }
    }

    private static final class MutableHit {
        private SearchHit best;
        private double score;

        private MutableHit(SearchHit best) {
            this.best = best;
        }

        private SearchHit best() {
            return best;
        }

        private double score() {
            return score;
        }
    }
}
