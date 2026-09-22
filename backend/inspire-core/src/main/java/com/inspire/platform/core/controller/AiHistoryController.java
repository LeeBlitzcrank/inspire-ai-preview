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
