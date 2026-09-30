/**
 * 文件：backend/inspire-search/src/main/java/com/inspire/platform/search/controller/SearchController.java
 * 所属模块：搜索服务模块，负责 MySQL/Elasticsearch 搜索及降级
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.search.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.impl.SearchServiceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "灵感搜索", description = "全文检索，支持ES+MySQL双引擎自动降级")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchServiceManager searchServiceManager;

    @Operation(summary = "搜索灵感", description = "关键词全文检索，mode=auto时自动尝试ES，ES不可用时降级为MySQL LIKE")
    @GetMapping("/public")
    public Result<List<SearchResultVO>> search(
            @Parameter(description = "搜索关键词", example = "鸡腿") @RequestParam String keyword,
            @Parameter(description = "分类筛选", example = "美食") @RequestParam(required = false) String tag,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "游标(search_after)，格式: heat_id") @RequestParam(required = false) String searchAfter,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=30, stale-while-revalidate=60");
        return Result.success(searchServiceManager.search(keyword, tag, page, size, searchAfter));
    }
}
