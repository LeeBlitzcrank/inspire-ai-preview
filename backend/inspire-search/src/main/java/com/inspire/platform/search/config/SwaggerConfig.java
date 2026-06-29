package com.inspire.platform.search.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI inspireSearchOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("灵思集 - 搜索服务")
                .description("全文检索：ES优先 + MySQL LIKE降级，inspire.search.mode=auto|es|mysql")
                .version("1.0.0"));
    }
}
