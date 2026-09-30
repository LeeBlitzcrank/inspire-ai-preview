/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/InspireAiApplication.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspireAiApplication.class, args);
    }
}
