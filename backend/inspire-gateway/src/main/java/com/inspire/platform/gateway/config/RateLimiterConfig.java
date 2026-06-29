package com.inspire.platform.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置
 *
 * 基于 Redis 令牌桶算法的 RequestRateLimiter。
 * - 默认：50 req/s，突发 100
 * - 各服务路由可在 application.yml 中独立调节
 *
 * PRD 7.5 安全：接口限流防刷
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 默认限流器：按 IP 限流
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter() {
        // replenishRate: 每秒填充令牌数（即每秒允许请求数）
        // burstCapacity: 令牌桶最大容量（即突发峰值请求数）
        // requestedTokens: 每次请求消耗令牌数
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * 按请求 IP 限流的 KeyResolver
     * 从 X-Forwarded-For 或远程 IP 获取客户端地址
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders()
                    .getFirst("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = exchange.getRequest().getRemoteAddress()
                        .getAddress().getHostAddress();
            }
            return Mono.just(ip);
        };
    }

    /**
     * 按用户 ID 限流的 KeyResolver（登录用户优先使用）
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                    .getFirst("X-Inspire-UserId");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            // 未登录用户降级为 IP 限流
            return ipKeyResolver().resolve(exchange);
        };
    }
}
