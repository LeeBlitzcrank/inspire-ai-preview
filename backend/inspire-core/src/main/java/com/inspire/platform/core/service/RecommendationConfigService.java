/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/RecommendationConfigService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：读取后台推荐配置，并提供短时间本地缓存
 * 维护说明：后台修改配置后最多延迟 30 秒生效，避免每次推荐都查询数据库。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationConfigService {

    private static final long CACHE_MS = 30_000;

    private final JdbcTemplate jdbcTemplate;
    private volatile RecommendationRuntimeConfig cached = RecommendationRuntimeConfig.defaults();
    private volatile long cachedAt = 0;

    public RecommendationRuntimeConfig current() {
        long now = System.currentTimeMillis();
        if (now - cachedAt <= CACHE_MS) return cached;
        synchronized (this) {
            if (now - cachedAt <= CACHE_MS) return cached;
            cached = load();
            cachedAt = now;
            return cached;
        }
    }

    public void invalidate() {
        cachedAt = 0;
    }

    private RecommendationRuntimeConfig load() {
        try {
            Map<String, String> values = new HashMap<>();
            jdbcTemplate.query("SELECT config_key,config_value FROM recommend_config",
                    (RowCallbackHandler) rs ->
                            values.put(rs.getString("config_key"), rs.getString("config_value")));
            return new RecommendationRuntimeConfig(
                    boolValue(values, "vector_recall_enabled", true),
                    intValue(values, "treatment_percent", 50, 0, 100),
                    intValue(values, "push_max_per_day", 1, 0, 10)
            );
        } catch (Exception e) {
            return RecommendationRuntimeConfig.defaults();
        }
    }

    private boolean boolValue(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        if (value == null || value.isBlank()) return fallback;
        return Boolean.parseBoolean(value.trim());
    }

    private int intValue(Map<String, String> values, String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
            return Math.max(min, Math.min(max, value));
        } catch (Exception e) {
            return fallback;
        }
    }

    public record RecommendationRuntimeConfig(
            boolean vectorRecallEnabled,
            int treatmentPercent,
            int pushMaxPerDay
    ) {
        public static RecommendationRuntimeConfig defaults() {
            return new RecommendationRuntimeConfig(true, 50, 1);
        }
    }
}
