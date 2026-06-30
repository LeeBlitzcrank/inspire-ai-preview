package com.inspire.platform.admin.config;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
@Configuration
@MapperScan("com.inspire.platform.admin.mapper")
public class MyBatisPlusConfig {}
