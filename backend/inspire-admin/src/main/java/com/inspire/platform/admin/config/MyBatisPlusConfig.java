/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/config/MyBatisPlusConfig.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.config;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
@Configuration
@MapperScan("com.inspire.platform.admin.mapper")
public class MyBatisPlusConfig {}
