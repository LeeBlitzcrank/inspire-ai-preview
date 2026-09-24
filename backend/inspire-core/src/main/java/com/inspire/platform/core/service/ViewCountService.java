package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 详情浏览量先写 Redis，定时批量落库，避免热门内容单行频繁更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private static final String PREFIX = "view:inspire:";
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public long increment(Long inspireId) {
        try {
            String key = PREFIX + inspireId;
            Long value = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofDays(2));
            return value == null ? -1L : value;
        } catch (Exception e) {
            log.debug("浏览量 Redis 计数失败: inspireId={}, {}", inspireId, e.getMessage());
            return -1L;
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void flush() {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(PREFIX + "*").count(500).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.debug("浏览量扫描失败: {}", e.getMessage());
            return;
        }

        int flushed = 0;
        for (String key : keys) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                long delta = value == null ? 0L : Long.parseLong(value);
                if (delta <= 0) {
                    redisTemplate.delete(key);
                    continue;
                }
                Long inspireId = Long.parseLong(key.substring(PREFIX.length()));
                int updated = jdbcTemplate.update(
                        "UPDATE inspire_main SET view_count = view_count + ? WHERE id = ?",
                        delta, inspireId);
                if (updated > 0) {
                    Long remaining = redisTemplate.opsForValue().decrement(key, delta);
                    if (remaining == null || remaining <= 0) {
                        redisTemplate.delete(key);
                    }
                    flushed++;
                }
            } catch (Exception e) {
                log.debug("浏览量落库失败: key={}, {}", key, e.getMessage());
            }
        }
        if (flushed > 0) {
            log.debug("浏览量批量落库完成: {}", flushed);
        }
    }
}
