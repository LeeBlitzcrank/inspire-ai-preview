package com.inspire.platform.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI inspireAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("灵思集 - AI灵感创作服务")
                        .description("关键词生成灵感、选择候选、发布至广场")
                        .version("1.0.0"));
    }
}
