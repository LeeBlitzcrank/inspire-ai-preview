package com.inspire.platform.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class SearchServiceManager implements SearchService {

    private final MysqlSearchService mysqlSearchService;
    private final EsSearchService esSearchService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String mode;
    private final long cacheTtlSeconds;

    public SearchServiceManager(
            @Qualifier("mysqlSearchService") MysqlSearchService mysqlSearchService,
            @Qualifier("esSearchService") EsSearchService esSearchService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${inspire.search.mode:auto}") String mode,
            @Value("${inspire.search.cache-ttl-seconds:30}") long cacheTtlSeconds) {
        this.mysqlSearchService = mysqlSearchService;
        this.esSearchService = esSearchService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.mode = mode;
        this.cacheTtlSeconds = Math.max(1, cacheTtlSeconds);
    }

    @PostConstruct
    public void init() {
        log.info("搜索模式: mode={}（mysql=仅MySQL, es=仅ES, auto=ES优先+MySQL降级）", mode);
    }

    @Override
    public List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        String cacheKey = cacheKey(keyword, tag, safePage, safeSize, searchAfter);
        List<SearchResultVO> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<SearchResultVO> results = doSearch(keyword, tag, safePage, safeSize, searchAfter);
        writeCache(cacheKey, results);
        return results;
    }

    private List<SearchResultVO> doSearch(String keyword, String tag, int page, int size, String searchAfter) {
        switch (mode) {
            case "mysql":
                return mysqlSearchService.search(keyword, tag, page, size, searchAfter);
            case "es":
                return esSearchService.search(keyword, tag, page, size, searchAfter);
            case "auto":
                return searchWithFallback(keyword, tag, page, size, searchAfter);
            default:
                throw new BusinessException("无效搜索模式: " + mode);
        }
    }

    private List<SearchResultVO> searchWithFallback(String keyword, String tag, int page, int size, String searchAfter) {
        try {
            return esSearchService.search(keyword, tag, page, size, searchAfter);
        } catch (Exception e) {
            log.warn("ES不可用，自动降级至MySQL搜索: {}", e.getMessage());
            return mysqlSearchService.search(keyword, tag, page, size, searchAfter);
        }
    }

    private String cacheKey(String keyword, String tag, int page, int size, String searchAfter) {
        return "search:v1:"
                + String.valueOf(keyword).trim() + ":"
                + (tag == null ? "" : tag.trim()) + ":"
                + page + ":" + size + ":"
                + (searchAfter == null ? "" : searchAfter);
    }

    private List<SearchResultVO> readCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<SearchResultVO>>() {});
        } catch (Exception e) {
            log.debug("搜索缓存读取失败: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, List<SearchResultVO> results) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(results),
                    Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception e) {
            log.debug("搜索缓存写入失败: {}", e.getMessage());
        }
    }
}
