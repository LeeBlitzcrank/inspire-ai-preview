/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/InspireCoreApplication.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@EnableCaching
@EnableScheduling
@EnableAsync
public class InspireCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspireCoreApplication.class, args);
    }
}
