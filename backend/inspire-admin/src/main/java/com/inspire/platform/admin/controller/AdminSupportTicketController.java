/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/controller/AdminSupportTicketController.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.controller;

import com.inspire.platform.admin.service.AdminSupportTicketService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "人工客服工单", description = "客服转人工工单处理")
@RestController
@RequestMapping("/admin/support")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final AdminSupportTicketService service;

    @Operation(summary = "工单列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.list(status, page, size));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(service.detail(id));
    }

    @Operation(summary = "认领工单")
    @PostMapping("/{id}/claim")
    public Result<Void> claim(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        service.claim(id, adminId);
        return Result.success("已认领", null);
    }

    @Operation(summary = "回复工单")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @Valid @RequestBody ReplyRequest request) {
        service.reply(id, adminId, request.content());
        return Result.success("回复成功", null);
    }

    @Operation(summary = "关闭工单")
    @PostMapping("/{id}/close")
    public Result<Void> close(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        service.close(id, adminId);
        return Result.success("已关闭", null);
    }

    public record ReplyRequest(
            @NotBlank(message = "回复内容不能为空")
            @Size(max = 2000, message = "回复内容不能超过2000个字符")
            String content
    ) {
    }
}
