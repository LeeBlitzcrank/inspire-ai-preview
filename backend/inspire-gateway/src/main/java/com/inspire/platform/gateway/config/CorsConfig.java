package com.inspire.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 网关跨域配置（CORS）
 *
 * 使用显式来源白名单（不使用通配符 "*"），避免
 * "Access-Control-Allow-Origin cannot contain more than one origin" 问题。
 * 生产域名通过环境变量 INSPIRE_CORS_ALLOWED_ORIGINS 注入（逗号分隔）。
 */
@Configuration
public class CorsConfig {

    @Value("${inspire.cors.allowed-origins:https://ai.20sherry.com,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 显式来源白名单（精确匹配，避免多 Origin 冲突）
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);

        // 允许的请求方法
        config.addAllowedMethod("*");

        // 允许的请求头（Authorization / Refresh-Token / Content-Type 等）
        config.addAllowedHeader("*");

        // 允许携带凭证（Cookie、Authorization头）
        config.setAllowCredentials(true);

        // 暴露给前端的响应头
        config.addExposedHeader("X-Cache-Status");

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
