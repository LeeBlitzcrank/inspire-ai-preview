package com.inspire.platform.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Token 黑名单服务
 * <p>
 * 用于查询登出/踢人后失效的 AccessToken 是否仍被黑名单缓存控制。
 * Redis Key 约定（对照文档 4.6 节）：
 *   Key: black_token:{accessToken}
 *   Value: 1
 *   TTL: JWT 剩余过期秒数
 * <p>
 * 本服务仅供网关拦截器调用，黑名单写入由 inspire-auth 服务通过 Redis 直接操作完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "black_token:";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * 判断指定 AccessToken 是否存在于黑名单中
     *
     * @param accessToken JWT AccessToken 字符串
     * @return true=已失效（黑名单命中） / false=正常
     */
    public Mono<Boolean> isBlacklisted(String accessToken) {
        String key = BLACKLIST_KEY_PREFIX + accessToken;
        return redisTemplate.hasKey(key)
                .doOnNext(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.warn("黑名单命中 token={}", maskToken(accessToken));
                    }
                });
    }

    /**
     * 向黑名单写入失效的 AccessToken（供网关调试 / 运维辅助使用）
     * 生产环境由 inspire-auth 写入，网关只读
     *
     * @param accessToken 待失效的 JWT
     * @param ttlSeconds  剩余存活秒数（过期后自动清理）
     */
    public Mono<Boolean> addToBlacklist(String accessToken, long ttlSeconds) {
        String key = BLACKLIST_KEY_PREFIX + accessToken;
        return redisTemplate.opsForValue()
                .set(key, "1", Duration.ofSeconds(ttlSeconds))
                .doOnNext(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.info("黑名单写入成功 token={}, ttl={}s", maskToken(accessToken), ttlSeconds);
                    }
                });
    }

    // ---- 辅助方法 ----

    /**
     * 脱敏展示 Token，仅显示前 20 位 + 后 10 位
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 30) {
            return token;
        }
        return token.substring(0, 20) + "..." + token.substring(token.length() - 10);
    }
}
