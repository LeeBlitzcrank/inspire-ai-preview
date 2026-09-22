package com.inspire.platform.auth.util;

import com.inspire.platform.common.constant.RedisKeyConstant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.Set;

/**
 * Redis 会话操作工具类
 * <p>
 * 文档 4.6 节 Redis Key 规范（统一使用冒号分隔）：
 * <pre>
 *   Key: refresh:{refreshToken}       Value: userId    TTL: 7天  刷新令牌映射
 *   Key: user_refresh:{userId}        Value: refreshToken TTL: 7天  用户绑定（踢人/SSO）
 *   Key: black_token:{accessToken}    Value: 1    TTL: JWT剩余秒 黑名单
 * </pre>
 */
@Component
public class RedisSessionUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionUtil.class);

    private static final String REFRESH_PREFIX = RedisKeyConstant.REFRESH_PREFIX;
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String BLACKLIST_PREFIX = RedisKeyConstant.BLACKLIST_PREFIX;

    private final StringRedisTemplate redisTemplate;

    @Value("${inspire.session.refresh-token-expiration:604800000}")
    private long refreshTokenTtlMs;

    public RedisSessionUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== RefreshToken 会话操作 ==========

    /**
     * 存入 refresh:{refreshToken} → userId，有效期 7 天
     */
    public void saveRefreshToken(String refreshToken, Long userId) {
        saveRefreshToken(refreshToken, userId, refreshTokenTtlMs);
    }

    public void saveRefreshToken(String refreshToken, Long userId, long ttlMs) {
        String key = REFRESH_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, String.valueOf(userId),
                ttlMs, TimeUnit.MILLISECONDS);
        log.debug("RefreshToken存入: key={}, userId={}, ttl={}ms", key, userId, ttlMs);
    }

    /**
     * 通过 refreshToken 查询绑定的 userId
     *
     * @return userId，不存在返回 null
     */
    public Long getUserIdByRefreshToken(String refreshToken) {
        String key = REFRESH_PREFIX + refreshToken;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : null;
    }

    /**
     * 删除 refresh:{refreshToken} 缓存
     */
    public void deleteRefreshToken(String refreshToken) {
        String key = REFRESH_PREFIX + refreshToken;
        redisTemplate.delete(key);
        log.debug("RefreshToken删除: key={}", key);
    }

    // ========== user_refresh 绑定操作（单点登录 & 踢人） ==========

    /**
     * 存入 user_refresh:{userId} → refreshToken，有效期 7 天
     */
    public void saveUserRefreshMapping(Long userId, String refreshToken) {
        saveUserRefreshMapping(userId, refreshToken, refreshTokenTtlMs);
    }

    public void saveUserRefreshMapping(Long userId, String refreshToken, long ttlMs) {
        String key = USER_SESSIONS_PREFIX + userId;
        redisTemplate.opsForSet().add(key, refreshToken);
        redisTemplate.expire(key, ttlMs, TimeUnit.MILLISECONDS);
        log.debug("用户会话集合加入: key={}, refreshToken={}", key, maskToken(refreshToken));
    }

    /**
     * 通过 userId 查询绑定的 refreshToken
     *
     * @return refreshToken，不存在返回 null
     */
    public String getUserRefreshToken(Long userId) {
        Set<String> tokens = redisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);
        return tokens == null || tokens.isEmpty() ? null : tokens.iterator().next();
    }

    /**
     * 删除 user_refresh:{userId} 缓存
     */
    public void deleteUserRefreshMapping(Long userId) {
        invalidateAllSessions(userId);
    }

    /** 退出单个标签页时只移除当前 refreshToken，不影响其他设备/标签页。 */
    public void removeUserRefreshToken(Long userId, String refreshToken) {
        if (refreshToken == null) return;
        redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + userId, refreshToken);
        log.debug("用户会话集合移除: userId={}, refreshToken={}", userId, maskToken(refreshToken));
    }

    /** 管理员踢人或账号冻结时清理该用户全部会话。 */
    public void invalidateAllSessions(Long userId) {
        String key = USER_SESSIONS_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(key);
        if (tokens != null) {
            tokens.forEach(this::deleteRefreshToken);
        }
        redisTemplate.delete(key);
        log.info("用户全部会话已清理: userId={}, count={}", userId, tokens == null ? 0 : tokens.size());
    }

    // ========== 单点登录（文档 4.1.2 第6步） ==========

    /**
     * 单点登录：新设备登录时挤掉旧设备
     * <p>
     * 1. 查询 user_refresh:{userId} 获取旧的 refreshToken
     * 2. 删除旧 refresh:{oldRefreshToken} 缓存
     * 3. 旧设备下次访问直接 401（黑名单配合网关拦截）
     *
     * @param userId 用户ID
     */
    public void invalidateOldSession(Long userId) {
        // 多设备登录模式下不再挤掉旧会话。
    }

    // ========== 黑名单操作 ==========

    /**
     * 将 AccessToken 加入黑名单，TTL 为 JWT 剩余存活秒数
     *
     * @param accessToken  JWT
     * @param ttlSeconds   剩余秒数
     */
    public void addToBlacklist(String accessToken, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + accessToken;
        redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
        log.info("黑名单写入: {}, ttl={}s", maskToken(accessToken), ttlSeconds);
    }

    /**
     * 清除用户所有会话缓存（登出/踢人共用）
     *
     * @param userId       用户ID
     * @param refreshToken 当前刷新令牌
     */
    public void clearUserSession(Long userId, String refreshToken) {
        deleteRefreshToken(refreshToken);
        removeUserRefreshToken(userId, refreshToken);
        log.info("当前用户会话清除: userId={}", userId);
    }

    // ========== 辅助方法 ==========

    /**
     * Token 脱敏显示
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 30) {
            return token;
        }
        return token.substring(0, 15) + "..." + token.substring(token.length() - 10);
    }
}
