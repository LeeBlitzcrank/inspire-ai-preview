/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/config/PublicCacheHeaderFilter.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：请求过滤组件，处理进入业务前或响应后的通用逻辑
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 给公开只读接口补上明确的浏览器/CDN缓存头。
 *
 * <p>Controller 上的 @Cacheable 命中时不会执行方法体，因此不能只在方法里 setHeader。
 * 这里在请求完成后统一设置，保证缓存命中时响应头也一致。</p>
 */
@Component
public class PublicCacheHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean loggedIn = request.getHeader("X-User-Id") != null;

        if (path.equals("/inspire/public/categories")
                || path.equals("/inspire/public/word-cloud")) {
            response.setHeader("Cache-Control", "public, max-age=600");
            response.setHeader("Cloudflare-CDN-Cache-Control", "public, max-age=600");
        } else if (!loggedIn && (
                path.equals("/inspire/public/recommend")
                || path.equals("/inspire/public/list"))) {
            response.setHeader("Cache-Control", "public, max-age=60");
            response.setHeader("Cloudflare-CDN-Cache-Control", "public, max-age=60");
        }

        filterChain.doFilter(request, response);
    }
}
