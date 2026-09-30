/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/util/ReactiveSessionRedisOps.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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