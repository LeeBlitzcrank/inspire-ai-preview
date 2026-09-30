/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/InspireAdminApplication.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：Spring Boot 应用启动入口，负责服务启动和组件扫描
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@EnableCaching
public class InspireAdminApplication {
    public static void main(String[] args) { SpringApplication.run(InspireAdminApplication.class, args); }
}
