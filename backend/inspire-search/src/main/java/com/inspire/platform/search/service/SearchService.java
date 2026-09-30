/**
 * 文件：backend/inspire-search/src/main/java/com/inspire/platform/search/service/SearchService.java
 * 所属模块：搜索服务模块，负责 MySQL/Elasticsearch 搜索及降级
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.search.service;

import com.inspire.platform.search.dto.SearchResultVO;

import java.util.List;
public interface SearchService {
    List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter);
}
