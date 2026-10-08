package com.inspire.platform.admin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class AdminAuditFilter extends OncePerRequestFilter {

    private static final Set<String> AUDITED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin/")
                || "/admin/login".equals(path)
                || path.startsWith("/admin/public/")
                || !AUDITED_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = request instanceof ContentCachingRequestWrapper caching
                ? caching
                : new ContentCachingRequestWrapper(request);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            try {
                long durationMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
                Long adminUserId = parseLong(wrapped.getHeader("X-User-Id"));
                String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
                if (body.length() > 5_000) {
                    body = body.substring(0, 5_000) + "...";
                }
                boolean success = response.getStatus() >= 200 && response.getStatus() < 400;
                jdbcTemplate.update(
                        "INSERT INTO admin_audit_log(id,admin_user_id,method,path,request_body,"
                                + "response_status,success,ip,user_agent,duration_ms,create_time) "
                                + "VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                        ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE),
                        adminUserId,
                        wrapped.getMethod(),
                        abbreviate(wrapped.getRequestURI(), 255),
                        body,
                        response.getStatus(),
                        success ? 1 : 0,
                        abbreviate(clientIp(wrapped), 64),
                        abbreviate(wrapped.getHeader("User-Agent"), 512),
                        durationMs,
                        LocalDateTime.now(ZONE));
            } catch (Exception e) {
                log.warn("后台审计日志写入失败: path={}, error={}",
                        request.getRequestURI(), e.getMessage());
            }
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
