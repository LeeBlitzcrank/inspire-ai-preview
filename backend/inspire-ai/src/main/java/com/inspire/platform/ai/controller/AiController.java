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

@Tag(name = "AI灵感创作", description = "关键词生成灵感、选择灵感、发布至广场")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI生成灵感", description = "输入关键词，AI返回2条差异化灵感标题卡片，支持精确/模糊/动态生成三种模式")
    @PostMapping("/generate")
    public Result<AiGenerateResponse> generate(
            @Parameter(description = "用户ID（由网关从JWT解析注入）")
            @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiGenerateRequest request) {
        request.setCity(request.getCity() == null ? "" : request.getCity());
        AiGenerateResponse response = aiService.generate(request);
        return Result.success(response);
    }

    @Operation(summary = "选中灵感", description = "用户从2条候选中选中一条，自动上报行为数据（关键词、选中灵感、城市、操作时间）")
    @PostMapping("/select")
    public Result<Void> select(
            @Parameter(description = "用户ID（由网关从JWT解析注入）")
            @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiSelectRequest request) {
        aiService.select(request, userId);
        return Result.success("灵感已选中，可保存草稿或发布", null);
    }

    @Operation(summary = "发布灵感", description = "将选中的灵感补充完整正文后公开发布至灵感广场，参与同城热点计算")
    @PostMapping("/publish")
    public Result<Void> publish(
            @Parameter(description = "用户ID（由网关从JWT解析注入）")
            @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody AiPublishRequest request) {
        aiService.publish(request, userId);
        return Result.success("灵感发布成功", null);
    }
}
