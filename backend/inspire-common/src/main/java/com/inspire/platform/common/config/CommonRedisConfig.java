package com.inspire.platform.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 通用 Redis 配置（下游业务微服务使用）
 * <p>
 * 文档 4.6 节：Redis 承担会话缓存、黑名单存储。
 * 序列化方案优化：
 * — Key: StringRedisSerializer（可读性好，兼容 Redis 命令行）
 * — Value: GenericJackson2JsonRedisSerializer（通用 JSON，支持泛型）
 * — 启用默认类型（LaissezFaireSubTypeValidator），支持多态反序列化
 * — 注册 JavaTimeModule 模块，支持 Java 8 日期时间类型
 * <p>
 * 条件：只有在 classpath 中存在 RedisConnectionFactory 时才启用。
 * 不依赖 Redis 的微服务不会加载此配置。
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class CommonRedisConfig {

    /**
     * StringRedisTemplate：黑名单 / 会话 KV 操作
     */
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate<String, Object>：通用序列化操作
     * <p>
     * 序列化方案：
     * - Key：StringRedisSerializer（冒号分隔的 Key，如 "black_token:{token}"）
     * - Value：GenericJackson2JsonRedisSerializer（比 Jackson2JsonRedisSerializer 更通用，支持容器类型）
     * - HashKey/Value 同 Key/Value
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
