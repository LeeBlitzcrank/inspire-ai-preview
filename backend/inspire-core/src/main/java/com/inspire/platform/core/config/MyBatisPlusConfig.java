/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/config/MyBatisPlusConfig.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.inspire.platform.core.mapper")
public class MyBatisPlusConfig implements MetaObjectHandler {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject meta) {
        this.strictInsertFill(meta, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(meta, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject meta) {
        this.strictUpdateFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
