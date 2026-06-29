package com.inspire.platform.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI inspireCoreOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("灵思集 - 灵感核心服务")
                .description("灵感CRUD、收藏点赞、草稿/发布管理")
                .version("1.0.0"));
    }
}
