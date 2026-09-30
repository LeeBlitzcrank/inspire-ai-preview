/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/config/SwaggerConfig.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
