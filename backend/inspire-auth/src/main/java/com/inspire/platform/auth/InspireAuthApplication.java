/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/InspireAuthApplication.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspireAuthApplication.class, args);
    }
}
