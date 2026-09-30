/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/config/UserContextWebMvcConfig.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.config;

import com.inspire.platform.common.interceptor.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${inspire.internal-auth.secret:}")
    private String internalAuthSecret;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor(internalAuthSecret))
                .addPathPatterns("/**")
                .order(1);  // 在业务拦截器之前执行，确保 UserContext 优先注入
    }
}
