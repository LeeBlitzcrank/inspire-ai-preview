/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/config/MinioConfig.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置（文档 3.1 节 / 文档 5.2 节）
 * <p>
 * 连接本地 Docker MinIO 实例，所有图片存取通过此客户端完成。
 * 密钥从 application.yml / 环境变量注入，禁止硬编码。
 */
@Configuration
public class MinioConfig {

    @Value("${inspire.minio.endpoint}")
    private String endpoint;

    @Value("${inspire.minio.access-key}")
    private String accessKey;

    @Value("${inspire.minio.secret-key}")
    private String secretKey;

    @Value("${inspire.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String getBucket() {
        return bucket;
    }
}
