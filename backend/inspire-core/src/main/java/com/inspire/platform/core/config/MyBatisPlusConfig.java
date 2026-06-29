package com.inspire.platform.core.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
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
        DynamicTableNameInnerInterceptor dynamic = new DynamicTableNameInnerInterceptor();
        dynamic.setTableNameHandler((sql, tableName) -> {
            String suffix = ShardContext.get();
            if (suffix != null) return tableName + "_" + suffix;
            return tableName;
        });
        interceptor.addInnerInterceptor(dynamic);
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
