package com.inspire.platform.search.service.impl;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SearchServiceManager implements SearchService {

    private final MysqlSearchService mysqlSearchService;
    private final EsSearchService esSearchService;
    private final String mode;

    public SearchServiceManager(
            @Qualifier("mysqlSearchService") MysqlSearchService mysqlSearchService,
            @Qualifier("esSearchService") EsSearchService esSearchService,
            @Value("${inspire.search.mode:auto}") String mode) {
        this.mysqlSearchService = mysqlSearchService;
        this.esSearchService = esSearchService;
        this.mode = mode;
    }

    @PostConstruct
    public void init() {
        log.info("搜索模式: mode={}（mysql=仅MySQL, es=仅ES, auto=ES优先+MySQL降级）", mode);
    }

    @Override
    public List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter) {
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
            List<SearchResultVO> results = esSearchService.search(keyword, tag, page, size, searchAfter);
            log.info("ES搜索成功，已降级检查通过");
            return results;
        } catch (Exception e) {
            log.warn("ES不可用，自动降级至MySQL搜索: {}", e.getMessage());
            return mysqlSearchService.search(keyword, tag, page, size, searchAfter);
        }
    }
}
