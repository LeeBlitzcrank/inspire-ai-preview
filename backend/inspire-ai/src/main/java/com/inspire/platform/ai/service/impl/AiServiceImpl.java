package com.inspire.platform.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.dto.*;
import com.inspire.platform.ai.service.AiService;
import com.inspire.platform.common.util.TitleUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    // 缓存 24 小时：同一关键词的结果可被其他用户直接复用，减少 token 消耗
    private static final int CACHE_TTL = 24 * 3600;
    // v3：标题限制为 16 字，换前缀让旧的长标题缓存立即失效
    private static final String CACHE_PREFIX = "ai:explore:v3:";
    private final JedisPool jedisPool;

    public AiServiceImpl(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${inspire.ai.deepseek.api-key}") String apiKey,
                         @Value("${inspire.ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}") String apiUrl,
                         @Value("${inspire.ai.deepseek.model:deepseek-chat}") String model,
                         JedisPool jedisPool) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.jedisPool = jedisPool;
    }

    // ==================== 原有接口 ====================

    @Override
    public AiGenerateResponse generate(AiGenerateRequest request) {
        String keyword = request.getKeyword().trim();
        AiExploreResponse resp = doExplore(keyword, null, false);
        if (resp.getOptions() != null && !resp.getOptions().isEmpty()) {
            AiExploreResponse.Option first = resp.getOptions().get(0);
            AiExploreResponse detail = doExplore(keyword, first.getId(), false);
            if (detail.getContent() != null) {
                AiExploreResponse.LeafContent c = detail.getContent();
                return new AiGenerateResponse(keyword, Arrays.asList(
                        new InspirationCandidate(1, c.getTitle(), c.getText(), c.getTag()),
                        new InspirationCandidate(2, resp.getSummary() + " — " + detail.getSummary(), "", resp.getContent() != null ? resp.getContent().getTag() : "其他")
                ));
            }
        }
        return new AiGenerateResponse(keyword, Collections.singletonList(
                new InspirationCandidate(1, keyword + "创意灵感", resp.getSummary(), "其他")));
    }

    @Override public void select(AiSelectRequest request, Long userId) {
        log.info("选中灵感: userId={}, keyword={}", userId, request.getKeyword());
    }

    @Override public void publish(AiPublishRequest request, Long userId) {
        log.info("发布灵感: userId={}, title={}", userId, request.getTitle());
    }

    // ==================== 探索接口（核心）====================

    @Override
    public AiExploreResponse explore(AiExploreRequest request) {
        int variants = request.getVariants() == null ? 3 : Math.max(1, Math.min(5, request.getVariants()));
        return doExplore(request.getKeyword(), request.getPath(), request.isRefresh(), variants);
    }

    private AiExploreResponse doExplore(String keyword, String path, boolean refresh) {
        return doExplore(keyword, path, refresh, 1);
    }

    private AiExploreResponse doExplore(String keyword, String path, boolean refresh, int variants) {
        String cacheKey = keyword + (path != null ? "|" + path : "");
        FetchResult fetched = getOrFetch(cacheKey, keyword, path, refresh, variants);
        String json = fetched.json;
        try {
            // 清理可能的markdown标记
            json = json.replaceAll("```json\\s*|```\\s*", "").trim();
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            AiExploreResponse resp = new AiExploreResponse();
            resp.setCacheKey(cacheKey);
            resp.setFromCache(fetched.fromCache);
            resp.setSummary((String) data.getOrDefault("summary", ""));

            List<Map<String, String>> optList = (List<Map<String, String>>) data.get("options");
            if (optList != null && !optList.isEmpty()) {
                List<AiExploreResponse.Option> options = new ArrayList<>();
                for (Map<String, String> o : optList) {
                    AiExploreResponse.Option opt = new AiExploreResponse.Option();
                    opt.setId(o.get("id"));
                    opt.setLabel(o.get("label"));
                    options.add(opt);
                }
                resp.setOptions(options);
            }

            Map<String, Object> contentMap = (Map<String, Object>) data.get("content");
            if (contentMap != null) {
                AiExploreResponse.LeafContent content = new AiExploreResponse.LeafContent();
                content.setTitle(TitleUtil.truncate((String) contentMap.get("title")));
                content.setText((String) contentMap.get("text"));
                content.setTag((String) contentMap.get("tag"));
                // 多风格候选：解析后主字段与第一组保持一致，兼容旧前端
                Object rawVariants = contentMap.get("variants");
                if (rawVariants instanceof List<?> vList && !vList.isEmpty()) {
                    List<AiExploreResponse.Variant> variantList = new ArrayList<>();
                    for (Object item : vList) {
                        if (!(item instanceof Map<?, ?> vm)) continue;
                        AiExploreResponse.Variant variant = new AiExploreResponse.Variant();
                        variant.setStyle(vm.get("style") == null ? null : String.valueOf(vm.get("style")));
                        variant.setTitle(vm.get("title") == null ? null : TitleUtil.truncate(String.valueOf(vm.get("title"))));
                        variant.setText(vm.get("text") == null ? null : String.valueOf(vm.get("text")));
                        variantList.add(variant);
                    }
                    if (!variantList.isEmpty()) {
                        content.setVariants(variantList);
                        AiExploreResponse.Variant first = variantList.get(0);
                        if (first.getTitle() != null) content.setTitle(first.getTitle());
                        if (first.getText() != null) content.setText(first.getText());
                    }
                }
                resp.setContent(content);
            }

            return resp;
        } catch (Exception e) {
            log.warn("解析失败: {}", e.getMessage());
            AiExploreResponse fallback = new AiExploreResponse();
            fallback.setCacheKey(cacheKey);
            fallback.setSummary("关于「" + keyword + "」的创意灵感");
            AiExploreResponse.Option opt = new AiExploreResponse.Option();
            opt.setId("create"); opt.setLabel("直接创作");
            fallback.setOptions(Collections.singletonList(opt));
            return fallback;
        }
    }

    /** 取缓存或调用模型；fromCache=true 表示本次没有消耗 token */
    private FetchResult getOrFetch(String cacheKey, String keyword, String path, boolean refresh, int variants) {
        FetchResult result = new FetchResult();
        String redisKey = CACHE_PREFIX + cacheKey;
        if (refresh) {
            log.info("换一批: keyword={}", keyword);
            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) { jedis.del(redisKey); }
            }
        }
        String cached = "";
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) { cached = jedis.get(redisKey); }
        }
        if (cached != null && !cached.isBlank()) {
            result.json = cached;
            result.fromCache = true;
            log.info("命中AI缓存（未消耗token）: {}", cacheKey);
            return result;
        }

        String prompt = buildPrompt(keyword, path, variants);
        log.info("DeepSeek请求: cacheKey={}", cacheKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Arrays.asList(
                Map.of("role", "system", "content",
                        "你是一个创意灵感生成器。返回JSON格式数据，不要markdown标记。\n" +
                        "当有子选项时返回：{\"summary\":\"概括\",\"options\":[{\"id\":\"xxx\",\"label\":\"选项\"}],\"content\":null}\n" +
                        "当到达最终内容时返回：{\"summary\":\"概括\",\"options\":[],\"content\":{\"title\":\"标题\",\"text\":\"详细内容\",\"tag\":\"分类\",\"variants\":[{\"style\":\"风格名\",\"title\":\"标题\",\"text\":\"正文\"}]}}\n" +
                        "硬性要求：最终内容的 text 必须是 300~500 个中文字符，分成 3~5 个自然段；" +
                        "其中至少一段用“1. 2. 3.”分条给出可执行的具体建议；语气自然、内容具体，不要空话套话。"),
                Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.8);
        body.put("max_tokens", 2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            String msg = (String) ((Map) ((List<Map>) resp.getBody().get("choices")).get(0).get("message")).get("content");
            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.setex(redisKey, CACHE_TTL, msg);
                    log.info("Redis缓存已写入: {}, TTL={}s", cacheKey, CACHE_TTL);
                }
            }
            // 异步记录 AI 调用
            try {
                Map<String, Object> callBody = new HashMap<>();
                callBody.put("keyword", keyword);
                callBody.put("userId", 0);
                new Thread(() -> {
                    try {
                        restTemplate.postForEntity("http://localhost:8083/internal/ai/call", callBody, String.class);
                    } catch (Exception ex) { /* 记录失败不影响主流程 */ }
                }).start();
            } catch (Exception ex) { /* 忽略 */ }
            result.json = msg;
            return result;
        } catch (Exception e) {
            log.warn("DeepSeek调用失败: {}", e.getMessage());
            // 降级返回
            result.json = "{\"summary\":\"关于「" + keyword + "」的创意灵感\",\"options\":[{\"id\":\"create\",\"label\":\"直接创作\"}],\"content\":null}";
            return result;
        }
    }

    /** 缓存读取结果：json + 是否命中缓存 */
    private static final class FetchResult {
        private String json = "";
        private boolean fromCache = false;
    }

    private String buildPrompt(String keyword, String path, int variants) {
        if (path == null || path.isEmpty()) {
            return "用户对「" + keyword + "」感兴趣。请返回关于「" + keyword + "」的3~5个探索方向。" +
                   "每个方向是一个子选项，用户会点击深入。返回JSON格式options。";
        }
        String[] parts = path.split(",");
        int depth = parts.length;
        String focus = parts[depth - 1];

        // 深度≤1返回子选项，深度≥2直接返回最终内容
        if (depth <= 1) {
            return "用户对「" + keyword + "」感兴趣，已选择：" + String.join(" > ", parts) +
                   "。请围绕「" + focus + "」返回3~5个子选项(options)，用JSON格式。";
        }
        // 深度≥2 → 直接返回最终内容，不再有options
        return "用户对「" + keyword + "」感兴趣，已选择：" + String.join(" > ", parts) +
               "。请围绕「" + focus + "」直接返回最终灵感内容(content)，不要options。" +
               "content.title 是 6~16 个中文字符的标题；content.text 必须是 300~500 个中文字符，" +
               "分成 3~5 个自然段，其中至少一段用“1. 2. 3.”分条列出可落地的建议，" +
               "内容要具体、有画面感，不要泛泛而谈；content.tag 从「家居/美食/旅行/摄影/穿搭/手作/运动/文案/电影/生活」中选一个。" +
               "另外必须在 content.variants 里一次给出 " + variants + " 组不同风格的文案，" +
               "每组格式 {\"style\":\"风格名\",\"title\":\"标题\",\"text\":\"正文\"}，" +
               "style 用「温柔治愈/干货清单/故事叙事/活泼种草」这类中文风格名，各组标题与正文必须明显不同，" +
               "每组的 text 同样满足 300~500 字的要求；content.title 与 content.text 取第一组的内容。";
    }
}
