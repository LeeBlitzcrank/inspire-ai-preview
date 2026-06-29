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
