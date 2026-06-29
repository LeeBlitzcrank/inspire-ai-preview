package com.inspire.platform.search.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.impl.SearchServiceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchServiceManager.search(keyword, tag, page, size));
    }
}
