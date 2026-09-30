/**
 * 文件：backend/inspire-gateway/src/main/java/com/inspire/platform/gateway/InspireGatewayApplication.java
 * 所属模块：API 网关模块，负责路由、CORS、限流、JWT 校验和可信身份透传
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspireGatewayApplication.class, args);
    }
}
