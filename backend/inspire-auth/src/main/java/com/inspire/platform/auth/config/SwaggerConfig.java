/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/config/SwaggerConfig.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI inspireOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("灵思集 AI灵感分享平台 - API")
                        .description("用户认证模块接口文档\n\n" +
                                "Base URL: http://localhost:8081 (直连) 或 http://localhost:8080/api/auth (通过网关)")
                        .version("1.0.0")
                        .contact(new Contact().name("Inspire AI").email("admin@inspire-ai.com")));
    }
}
