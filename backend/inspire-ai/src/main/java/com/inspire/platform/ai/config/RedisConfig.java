package com.inspire.platform.ai.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {
    @Bean
    public JedisPool jedisPool(@Value("${redis.host:localhost}") String host,
                                @Value("${redis.port:6379}") int port,
                                @Value("${redis.password:}") String password) {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(10);
        cfg.setMaxIdle(5);
        try {
            JedisPool pool = new JedisPool(cfg, host, port, 3000, password);
            pool.getResource().close(); // 验证连接
            return pool;
        } catch (Exception e) {
            return null; // Redis 不可用，走直连
        }
    }
}
