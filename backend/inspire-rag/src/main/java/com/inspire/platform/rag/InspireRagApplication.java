/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/InspireRagApplication.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@ConfigurationPropertiesScan
@EnableScheduling
public class InspireRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspireRagApplication.class, args);
    }
}
