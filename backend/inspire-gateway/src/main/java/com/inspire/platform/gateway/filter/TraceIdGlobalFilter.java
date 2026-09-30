/**
 * 文件：backend/inspire-gateway/src/main/java/com/inspire/platform/gateway/filter/TraceIdGlobalFilter.java
 * 所属模块：API 网关模块，负责路由、CORS、限流、JWT 校验和可信身份透传
 * 主要职责：请求过滤组件，处理进入业务前或响应后的通用逻辑
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || !SAFE_TRACE_ID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalTraceId = traceId;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(TRACE_ID_HEADER, finalTraceId))
                .build();
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, finalTraceId);
            return Mono.empty();
        });
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -300;
    }
}
