package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容写入后的统一缓存失效入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheService {

    private static final List<String> PUBLIC_CACHES = List.of(
            "publicList", "recommend", "hotTags", "categories", "wordCloud");

    private final CacheManager cacheManager;

    public void evictPublicContent() {
        for (String name : PUBLIC_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        log.debug("公共内容缓存已清理");
    }
}
