package com.inspire.platform.common.config;

import com.inspire.platform.common.interceptor.UserContextInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户上下文拦截器自动注册
 * <p>
 * 所有依赖 inspire-common 的业务微服务自动注册该拦截器，
 * 在每个 HTTP 请求的 Controller 处理之前注入 {@code UserContext}。
 * <p>
 * 注意：此配置类会与各业务模块自身的 WebMvcConfigurer 共存，
 * Spring Boot 会自动合并多个 WebMvcConfigurer 实现。
 *
 * @see com.inspire.platform.common.interceptor.UserContextInterceptor
 */
@Configuration
@ConditionalOnWebApplication
public class UserContextWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**")
                .order(1);  // 在业务拦截器之前执行，确保 UserContext 优先注入
    }
}
