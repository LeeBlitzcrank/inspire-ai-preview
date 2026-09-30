/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/controller/AiController.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.controller;

import com.inspire.platform.ai.dto.*;
import com.inspire.platform.ai.service.AiService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI灵感创作")
@RestController @RequestMapping("/ai") @RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @Operation(summary = "AI探索灵感", description = "输入关键词，AI返回分层选项，逐步深入直到获取完整内容")
    @PostMapping("/explore")
    public Result<AiExploreResponse> explore(@Valid @RequestBody AiExploreRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(aiService.explore(request));
    }

    @Operation(summary = "AI改写选中正文")
    @PostMapping("/rewrite")
    public Result<AiRewriteResponse> rewrite(@Valid @RequestBody AiRewriteRequest request,
                                             @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(aiService.rewrite(request));
    }

    @Operation(summary = "AI生成标题候选")
    @PostMapping("/titles")
    public Result<AiTitleResponse> titles(@Valid @RequestBody AiTitleRequest request,
                                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(aiService.titles(request));
    }

    @Operation(summary = "AI生成灵感（旧版兼容）")
    @PostMapping("/generate")
    public Result<AiGenerateResponse> generate(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AiGenerateRequest request) {
        request.setCity(request.getCity() == null ? "" : request.getCity());
        return Result.success(aiService.generate(request));
    }

    @Operation(summary = "选中灵感")
    @PostMapping("/select")
    public Result<Void> select(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AiSelectRequest request) {
        aiService.select(request, userId);
        return Result.success("灵感已选中", null);
    }

    @Operation(summary = "发布灵感")
    @PostMapping("/publish")
    public Result<Void> publish(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AiPublishRequest request) {
        aiService.publish(request, userId);
        return Result.success("灵感发布成功", null);
    }
}
