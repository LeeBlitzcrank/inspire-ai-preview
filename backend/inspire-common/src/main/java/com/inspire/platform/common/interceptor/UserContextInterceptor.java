package com.inspire.platform.common.interceptor;

import com.inspire.platform.common.model.UserContext;
import com.inspire.platform.common.util.InternalAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 用户上下文拦截器
 * <p>
 * 文档 6.3 节：下游业务服务不解析JWT，通过网关透传的请求头获取用户信息。
 * <p>
 * 在每个 HTTP 请求到达 Controller 前：
 * <ol>
 *   <li>从请求头读取 {@code X-User-Id} / {@code X-User-Role}</li>
 *   <li>注入 {@link UserContext} ThreadLocal</li>
 *   <li>请求完成后自动清理 ThreadLocal，防止内存泄漏</li>
 * </ol>
 * <p>
 * <b>MQ 消费者 / 定时任务：</b> 这些场景没有 HTTP 请求上下文，
 * 请求头不存在，拦截器不会注入用户信息，UserContext.isLogin() 返回 false。
 * 定时任务需手动调用 {@code UserContext.setUserId()} 指定内部系统标识。
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    private final String internalAuthSecret;

    public UserContextInterceptor(String internalAuthSecret) {
        this.internalAuthSecret = internalAuthSecret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String userIdStr = request.getHeader(InternalAuthUtil.USER_ID_HEADER);
        String role = request.getHeader(InternalAuthUtil.USER_ROLE_HEADER);
        String timestamp = request.getHeader(InternalAuthUtil.TIMESTAMP_HEADER);
        String signature = request.getHeader(InternalAuthUtil.SIGNATURE_HEADER);

        boolean hasIdentity = StringUtils.hasText(userIdStr)
                || StringUtils.hasText(role)
                || StringUtils.hasText(timestamp)
                || StringUtils.hasText(signature);

        if (!hasIdentity) {
            // 无用户信息：可能是白名单接口或MQ消费端，跳过注入
            log.trace("用户上下文: 无用户信息, path={}", request.getRequestURI());
            return true;
        }

        if (!StringUtils.hasText(userIdStr)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)) {
            writeUnauthorized(response);
            return false;
        }

        String query = request.getQueryString();
        boolean trusted = InternalAuthUtil.verify(
                internalAuthSecret,
                userIdStr,
                role,
                timestamp,
                signature,
                request.getMethod(),
                request.getRequestURI(),
                query);
        if (!trusted) {
            log.warn("用户上下文: 内部身份签名无效, path={}", request.getRequestURI());
            writeUnauthorized(response);
            return false;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            UserContext.setUserId(userId);
            UserContext.setRole(role != null ? role : "");
            log.debug("用户上下文注入: userId={}, role={}, path={}",
                    userId, role, request.getRequestURI());
        } catch (NumberFormatException e) {
            log.warn("用户上下文: X-User-Id格式非法: {}", userIdStr);
        }

        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"code\":401001,\"msg\":\"内部身份校验失败\",\"data\":null}");
        } catch (IOException ignored) {
            // 响应已断开时无需额外处理
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 请求完成后必须清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
