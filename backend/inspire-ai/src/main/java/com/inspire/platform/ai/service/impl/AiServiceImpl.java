package com.inspire.platform.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.dto.*;
import com.inspire.platform.ai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    private static final int CACHE_TTL = 3600; // 缓存1小时
    private static final String CACHE_PREFIX = "ai:explore:";
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
        return doExplore(request.getKeyword(), request.getPath(), request.isRefresh());
    }

    private AiExploreResponse doExplore(String keyword, String path, boolean refresh) {
        String cacheKey = keyword + (path != null ? "|" + path : "");
        String json = getOrFetch(cacheKey, keyword, path, refresh);
        try {
            // 清理可能的markdown标记
            json = json.replaceAll("```json\\s*|```\\s*", "").trim();
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            AiExploreResponse resp = new AiExploreResponse();
            resp.setCacheKey(cacheKey);
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
                content.setTitle((String) contentMap.get("title"));
                content.setText((String) contentMap.get("text"));
                content.setTag((String) contentMap.get("tag"));
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

    private String getOrFetch(String cacheKey, String keyword, String path, boolean refresh) {
        String redisKey = CACHE_PREFIX + cacheKey;
        if (refresh) {
            log.info("换一批: keyword={}", keyword);
            try (Jedis jedis = jedisPool.getResource()) { jedis.del(redisKey); }
        }
        String cached;
        try (Jedis jedis = jedisPool.getResource()) { cached = jedis.get(redisKey); }
        if (cached != null) return cached;

        String prompt = buildPrompt(keyword, path);
        log.info("DeepSeek请求: cacheKey={}", cacheKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Arrays.asList(
                Map.of("role", "system", "content",
                        "你是一个创意灵感生成器。返回JSON格式数据，不要markdown标记。\n" +
                        "当有子选项时返回：{\"summary\":\"概括\",\"options\":[{\"id\":\"xxx\",\"label\":\"选项\"}],\"content\":null}\n" +
                        "当到达最终内容时返回：{\"summary\":\"概括\",\"options\":[],\"content\":{\"title\":\"标题\",\"text\":\"详细内容\",\"tag\":\"分类\"}}"),
                Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.8);
        body.put("max_tokens", 800);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            String msg = (String) ((Map) ((List<Map>) resp.getBody().get("choices")).get(0).get("message")).get("content");
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(redisKey, CACHE_TTL, msg);
                log.info("Redis缓存已写入: {}, TTL={}s", cacheKey, CACHE_TTL);
            }
            return msg;
        } catch (Exception e) {
            log.warn("DeepSeek调用失败: {}", e.getMessage());
            // 降级返回
            return "{\"summary\":\"关于「" + keyword + "」的创意灵感\",\"options\":[{\"id\":\"create\",\"label\":\"直接创作\"}],\"content\":null}";
        }
    }

    private String buildPrompt(String keyword, String path) {
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
               "。请围绕「" + focus + "」直接返回最终灵感内容(content)，不要options。";
    }
}
