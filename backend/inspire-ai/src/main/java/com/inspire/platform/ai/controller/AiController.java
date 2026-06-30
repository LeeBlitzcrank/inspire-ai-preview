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
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        return Result.success(aiService.explore(request));
    }

    @Operation(summary = "AI生成灵感（旧版兼容）")
    @PostMapping("/generate")
    public Result<AiGenerateResponse> generate(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiGenerateRequest request) {
        request.setCity(request.getCity() == null ? "" : request.getCity());
        return Result.success(aiService.generate(request));
    }

    @Operation(summary = "选中灵感")
    @PostMapping("/select")
    public Result<Void> select(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiSelectRequest request) {
        aiService.select(request, userId);
        return Result.success("灵感已选中", null);
    }

    @Operation(summary = "发布灵感")
    @PostMapping("/publish")
    public Result<Void> publish(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiPublishRequest request) {
        aiService.publish(request, userId);
        return Result.success("灵感发布成功", null);
    }
}
