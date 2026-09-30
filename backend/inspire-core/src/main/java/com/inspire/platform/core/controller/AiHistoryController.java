/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/controller/AiHistoryController.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.service.AiHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspire/ai/history")
@RequiredArgsConstructor
public class AiHistoryController {

    private final AiHistoryService aiHistoryService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(aiHistoryService.list(userId, limit));
    }

    @PostMapping
    public Result<Map<String, Object>> save(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        return Result.success(aiHistoryService.save(userId, body));
    }

    @PutMapping("/{id}/select")
    public Result<Void> select(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer selectedIndex = body.get("selectedIndex") == null
                ? -1 : Integer.valueOf(String.valueOf(body.get("selectedIndex")));
        String selectedTitle = body.get("selectedTitle") == null
                ? "" : String.valueOf(body.get("selectedTitle"));
        aiHistoryService.selectVariant(userId, id, selectedIndex, selectedTitle);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        aiHistoryService.delete(userId, id);
        return Result.success();
    }
}
