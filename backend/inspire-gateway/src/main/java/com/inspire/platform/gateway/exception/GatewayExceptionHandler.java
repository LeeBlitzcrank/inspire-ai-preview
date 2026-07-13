package com.inspire.platform.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器
 * <p>
 * 捕获所有未被过滤器/路由处理程序拦截的异常，按文档 4.2.3 标准 JSON 格式返回。
 * 优先级低于 JwtAuthGlobalFilter（-200），捕获上游未处理的异常。
 */
@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // ResponseStatusException：已知 HTTP 状态码异常（如路由 404、405）
        if (ex instanceof ResponseStatusException statusException) {
            HttpStatus status = HttpStatus.resolve(statusException.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                log.warn("网关 4xx 异常: status={}, path={}, msg={}",
                        statusException.getStatusCode(),
                        exchange.getRequest().getURI().getPath(),
                        statusException.getReason());

                if (status == HttpStatus.NOT_FOUND) {
                    return jsonResponse(response, HttpStatus.NOT_FOUND,
                            Result.error(404, "请求的接口不存在"));
                }
                return jsonResponse(response, status,
                        Result.error(status.value(), statusException.getReason()));
            }

            // 5xx 降级
            log.error("网关 5xx 异常: status={}, path={}",
                    statusException.getStatusCode(),
                    exchange.getRequest().getURI().getPath(), ex);
            return jsonResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    Result.error(500, "服务异常，请稍后重试"));
        }

        // 未知异常
        log.error("网关未预期异常: path={}", exchange.getRequest().getURI().getPath(), ex);
        return jsonResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                Result.error(500, "服务异常，请稍后重试"));
    }

    /**
     * 写入标准 JSON 错误响应
     */
    private Mono<Void> jsonResponse(ServerHttpResponse response, HttpStatus status, Result<?> body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Error-Handled", "gateway");

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":500,\"msg\":\"服务异常\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
