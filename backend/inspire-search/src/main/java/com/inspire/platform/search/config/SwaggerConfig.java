/**
 * 文件：backend/inspire-search/src/main/java/com/inspire/platform/search/config/SwaggerConfig.java
 * 所属模块：搜索服务模块，负责 MySQL/Elasticsearch 搜索及降级
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
