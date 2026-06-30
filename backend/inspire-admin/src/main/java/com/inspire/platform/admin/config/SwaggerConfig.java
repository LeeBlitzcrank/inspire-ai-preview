package com.inspire.platform.admin.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI adminOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("灵思集 - 管理员后台")
                .description("管理员登录、监控、灵感管理、推送配置")
                .version("1.0.0"));
    }
}
