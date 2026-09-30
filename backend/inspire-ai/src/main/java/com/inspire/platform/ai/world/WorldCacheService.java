/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldCacheService.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldCacheService {

    private static final String VERSION_KEY = "world:cache:version";
    private static final String PREFIX = "world:cache:v";

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private volatile String cachedVersion = "1";
    private volatile long versionLoadedAt;

    public <T> T get(String key, Class<T> type) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(cacheKey(jedis, key));
            return value == null ? null : objectMapper.readValue(value, type);
        } catch (Exception e) {
            log.debug("世界缓存读取失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> itemType) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(cacheKey(jedis, key));
            if (value == null) return null;
            return objectMapper.readValue(
                    value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, itemType)
            );
        } catch (Exception e) {
            log.debug("世界缓存列表读取失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void put(String key, Object value, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(cacheKey(jedis, key), ttlSeconds, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.debug("世界缓存写入失败 key={}: {}", key, e.getMessage());
        }
    }

    public void invalidateAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            long version = jedis.incr(VERSION_KEY);
            cachedVersion = Long.toString(version);
            versionLoadedAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("世界缓存版本更新失败: {}", e.getMessage());
        }
    }

    private String cacheKey(Jedis jedis, String key) {
        return PREFIX + version(jedis) + ":" + key;
    }

    private String version(Jedis jedis) {
        long now = System.currentTimeMillis();
        if (now - versionLoadedAt < 1000) return cachedVersion;
        String version = jedis.get(VERSION_KEY);
        if (version == null) {
            jedis.setnx(VERSION_KEY, "1");
            version = jedis.get(VERSION_KEY);
        }
        cachedVersion = version == null ? "1" : version;
        versionLoadedAt = now;
        return cachedVersion;
    }
}
