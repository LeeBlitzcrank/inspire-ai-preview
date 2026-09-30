/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/config/SwaggerConfig.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
