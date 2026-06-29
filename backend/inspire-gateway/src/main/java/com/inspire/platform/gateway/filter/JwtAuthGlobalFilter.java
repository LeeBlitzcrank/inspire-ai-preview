package com.inspire.platform.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 全局鉴权过滤器
 *
 * 白名单路径直接放行，非白名单路径校验 JWT Token。
 * 校验通过后将 userId / username 写入请求头，透传给下游微服务。
 *
 * PRD 2.1 登录鉴权规则：
 * - 未登录权限限制：仅可浏览广场灵感，不可使用AI生成、发布、收藏、点赞、接收推送
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 白名单路径：无需登录即可访问 */
    private final List<String> whiteList;

    public JwtAuthGlobalFilter(
            JwtUtil jwtUtil,
            ObjectMapper objectMapper,
            @Value("${inspire.jwt.white-list:/api/auth/**,/api/inspire/public/**}") String whiteListStr) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.whiteList = Arrays.asList(whiteListStr.split(","));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 1. 白名单路径直接放行，但仍尝试解析Token设置用户信息
        if (isWhiteList(path)) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                Claims claims = jwtUtil.validateToken(authHeader.substring(7));
                if (claims != null) {
                    request = request.mutate()
                        .header("X-Inspire-UserId", claims.getSubject())
                        .header("X-Inspire-Username", claims.get("username", String.class) == null ? "" : claims.get("username", String.class))
                        .build();
                }
            }
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 2. 从请求头获取 Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少有效的Authorization头: {}", path);
            return unauthorized(response, "未登录，请先登录账号");
        }

        String token = authHeader.substring(7);

        // 3. 校验 Token
        Claims claims = jwtUtil.validateToken(token);
        if (claims == null) {
            log.warn("JWT校验失败: {}", path);
            return unauthorized(response, "登录已过期，请重新登录");
        }

        // 4. 将用户信息写入请求头，透传给下游服务
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Inspire-UserId", userId)
                .header("X-Inspire-Username", username == null ? "" : username)
                .build();

        log.debug("JWT鉴权通过: userId={}, path={}", userId, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 优先级最高，在限流过滤器之前执行
        return -200;
    }

    /**
     * 判断是否属于白名单路径
     */
    private boolean isWhiteList(String path) {
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<?> result = Result.unauthorized(msg);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":401,\"msg\":\"未授权\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
