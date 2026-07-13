package com.inspire.platform.common.interceptor;

import com.inspire.platform.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

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

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 从请求头读取网关透传的用户信息
        String userIdStr = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (!StringUtils.hasText(userIdStr)) {
            // 无用户信息：可能是白名单接口或MQ消费端，跳过注入
            log.trace("用户上下文: 无用户信息, path={}", request.getRequestURI());
            return true;
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

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 请求完成后必须清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
