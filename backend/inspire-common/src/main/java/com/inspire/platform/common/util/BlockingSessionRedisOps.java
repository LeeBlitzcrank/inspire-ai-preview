package com.inspire.platform.common.util;

import com.inspire.platform.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 阻塞式Redis会话工具，仅普通Web服务(auth)使用
 * 条件注解：只有引入spring-data-redis、存在StringRedisTemplate才会实例化Bean
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(StringRedisTemplate.class)
public class BlockingSessionRedisOps {

    private final StringRedisTemplate redisTemplate;

    /** RefreshToken TTL 7天 毫秒 */
    private long refreshTokenTtlMs = 604800000;

    // ========== refresh:{rt} → userId ==========
    public void saveRefreshToken(String refreshToken, Long userId) {
        redisTemplate.opsForValue().set(
                RedisKeyConstant.refreshKey(refreshToken),
                String.valueOf(userId),
                refreshTokenTtlMs, TimeUnit.MILLISECONDS);
    }

    public Long getUserIdByRefreshToken(String refreshToken) {
        String val = redisTemplate.opsForValue().get(RedisKeyConstant.refreshKey(refreshToken));
        return val != null ? Long.parseLong(val) : null;
    }

    public void deleteRefreshToken(String refreshToken) {
        redisTemplate.delete(RedisKeyConstant.refreshKey(refreshToken));
    }

    // ========== user_refresh:{uid} → refreshToken ==========
    public void saveUserRefreshMapping(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                RedisKeyConstant.userRefreshKey(userId),
                refreshToken,
                refreshTokenTtlMs, TimeUnit.MILLISECONDS);
    }

    public String getUserRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(RedisKeyConstant.userRefreshKey(userId));
    }

    public void deleteUserRefreshMapping(Long userId) {
        redisTemplate.delete(RedisKeyConstant.userRefreshKey(userId));
    }

    // ========== SSO挤下线 ==========
    public void invalidateOldSession(Long userId) {
        String oldRt = getUserRefreshToken(userId);
        if (oldRt != null) {
            deleteRefreshToken(oldRt);
            log.info("SSO挤旧会话: userId={}", userId);
        }
    }

    // ========== 令牌黑名单 ==========
    public void addToBlacklist(String accessToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                RedisKeyConstant.blacklistKey(accessToken),
                "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstant.blacklistKey(accessToken)));
    }

    // ========== 清空用户全部会话 ==========
    public void clearUserSession(Long userId, String refreshToken) {
        deleteRefreshToken(refreshToken);
        deleteUserRefreshMapping(userId);
    }

    public void setRefreshTokenTtlMs(long ttlMs) {
        this.refreshTokenTtlMs = ttlMs;
    }
}