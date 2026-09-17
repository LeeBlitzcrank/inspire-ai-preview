package com.inspire.platform.common.util;

import com.inspire.platform.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 响应式Redis会话工具，仅gateway WebFlux使用
 * 条件注解：存在ReactiveStringRedisTemplate才实例化
 */
@Component
@RequiredArgsConstructor
@ConditionalOnClass(ReactiveStringRedisTemplate.class)
public class ReactiveSessionRedisOps {

    private final ReactiveStringRedisTemplate reactiveRedisTemplate;

    /** 网关鉴权：判断AccessToken是否在黑名单 */
    public Mono<Boolean> isBlacklisted(String accessToken) {
        return reactiveRedisTemplate.hasKey(RedisKeyConstant.blacklistKey(accessToken));
    }
}