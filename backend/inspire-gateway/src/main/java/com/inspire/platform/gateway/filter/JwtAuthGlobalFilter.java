package com.inspire.platform.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.gateway.model.ErrorCode;
import com.inspire.platform.gateway.service.TokenBlacklistService;
import com.inspire.platform.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
 * JWT 全局鉴权过滤器 —— 严格对照文档 4.2.2 鉴权校验顺序
 * <p>
 * 校验顺序（严格从上到下，不可颠倒）：
 * <ol>
 *   <li>判断请求路径是否属于白名单 → 是则直接放行，跳过后续所有鉴权</li>
 *   <li>校验请求头是否携带 {@code Authorization: Bearer {token}} → 否则 401001</li>
 *   <li>校验 Redis 黑名单 {@code black_token:{} → 命中则 401002</li>
 *   <li>校验 JWT 签名（HS256 全局密钥）→ 篡改则 401003</li>
 *   <li>校验 JWT 过期时间 → 过期则 401004</li>
 *   <li>解析载荷写入 {@code X-User-Id} / {@code X-User-Role} 请求头，透传下游</li>
 *   <li>转发请求至业务微服务</li>
 * </ol>
 * <p>
 * 响应格式严格遵照文档 4.2.3：
 * <pre>{@code {"code": 401004, "msg": "登录令牌已过期，请刷新令牌", "data": null}}</pre>
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 白名单路径：无需登录即可访问 */
    private final List<String> whiteList;

    public JwtAuthGlobalFilter(
            JwtUtil jwtUtil,
            TokenBlacklistService blacklistService,
            ObjectMapper objectMapper,
            @Value("${inspire.jwt.white-list}") String whiteListStr) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
        this.objectMapper = objectMapper;
        this.whiteList = Arrays.asList(whiteListStr.split(","));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // ==================================================================
        // 步骤①：白名单判断 —— 文档 4.2.2 第1步
        // 白名单接口直接放行，跳过鉴权
        // ==================================================================
        if (isWhiteList(path)) {
            log.debug("[鉴权] 白名单放行: {}", path);
            return chain.filter(exchange);
        }

        // ==================================================================
        // 步骤②：校验请求头 Authorization —— 文档 4.2.2 第2步
        // 缺失 → 返回 401001（未携带登录令牌）
        // ==================================================================
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("[鉴权] 缺少Authorization头: {}", path);
            return unauthorizedResponse(response, ErrorCode.TOKEN_MISSING);
        }

        String token = authHeader.substring(7);

        // ==================================================================
        // 步骤③：Redis 黑名单校验 —— 文档 4.2.2 第3步
        // black_token:{accessToken} 命中 → 返回 401002（令牌已失效）
        // 使用 Reactor 非阻塞链路
        // ==================================================================
        return blacklistService.isBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.warn("[鉴权] 黑名单命中: path={}", path);
                        return unauthorizedResponse(response, ErrorCode.TOKEN_INVALIDATED);
                    }

                    // ==========================================================
                    // 步骤④ + 步骤⑤：JWT 签名校验 & 过期校验 —— 文档 4.2.2 第4-5步
                    // ExpiredJwtException → 401004（令牌过期）
                    // 其他 JwtException     → 401003（签名篡改/非法）
                    // 校验通过 → 步6
                    // ==========================================================
                    try {
                        // parseToken 会校验签名 & 过期时间
                        // 签名失败 → JwtException
                        // 过期 → ExpiredJwtException（它是 JwtException 的子类，先捕获）
                        Claims claims = jwtUtil.parseToken(token);

                        // ======================================================
                        // 步骤⑥：透传用户 Header —— 文档 4.2.2 第6步
                        // X-User-Id: userId
                        // X-User-Role: admin / core / user
                        // ======================================================
                        String userId = claims.getSubject();
                        String role = claims.get("role", String.class);

                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-User-Id", userId)
                                .header("X-Inspire-UserId", userId)
                                .header("X-User-Role", role != null ? role : "")
                                .build();

                        log.debug("[鉴权] 通过: userId={}, role={}, path={}", userId, role, path);

                        // ======================================================
                        // 步骤⑦：转发请求至业务微服务 —— 文档 4.2.2 第7步
                        // ======================================================
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());

                    } catch (ExpiredJwtException e) {
                        // 文档 4.2.2 第5步：过期 → 401004
                        log.warn("[鉴权] 令牌过期: path={}, userId={}",
                                path, e.getClaims() != null ? e.getClaims().getSubject() : "?");
                        return unauthorizedResponse(response, ErrorCode.TOKEN_EXPIRED);

                    } catch (JwtException e) {
                        // 文档 4.2.2 第4步：签名非法/篡改 → 401003
                        log.warn("[鉴权] 签名校验失败: path={}, error={}", path, e.getMessage());
                        return unauthorizedResponse(response, ErrorCode.TOKEN_SIGNATURE_INVALID);
                    }
                });
    }

    @Override
    public int getOrder() {
        // 优先级最高（Ordered.LOWEST_PRECEDENCE - 自定义高位），在限流过滤器之前执行
        return -200;
    }

    // ======================================================================
    //  私有方法
    // ======================================================================

    /**
     * Ant 风格路径匹配
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
     * 封装标准 401 错误响应（文档 4.2.3 格式）
     * <pre>{@code {"code": 401xxx, "msg": "xxx", "data": null}}</pre>
     */
    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, ErrorCode errorCode) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 严格按照文档 4.2.3 标准错误码格式
        Result<?> result = Result.error(errorCode.getCode(), errorCode.getMessage());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + errorCode.getCode()
                    + ",\"msg\":\"" + errorCode.getMessage()
                    + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
