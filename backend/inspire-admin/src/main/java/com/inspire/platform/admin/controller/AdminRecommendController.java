/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/controller/AdminRecommendController.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.controller;

import com.inspire.platform.admin.dto.RecommendConfigUpdateRequest;
import com.inspire.platform.admin.dto.RecommendPushRequest;
import com.inspire.platform.admin.entity.RecommendConfig;
import com.inspire.platform.admin.entity.RecommendPush;
import com.inspire.platform.admin.service.AdminRecommendService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "推荐管理", description = "推荐配置、人工推送和效果指标")
@RestController
@RequestMapping("/admin/recommend")
@RequiredArgsConstructor
public class AdminRecommendController {

    private final AdminRecommendService service;

    @Operation(summary = "推荐配置列表")
    @GetMapping("/config")
    public Result<List<RecommendConfig>> configList() {
        return Result.success(service.configList());
    }

    @Operation(summary = "更新推荐配置")
    @PutMapping("/config")
    public Result<Void> updateConfigs(
            @Valid @RequestBody List<RecommendConfigUpdateRequest> requests) {
        service.updateConfigs(requests);
        return Result.success("配置已更新", null);
    }

    @Operation(summary = "人工推送列表")
    @GetMapping("/push")
    public Result<List<RecommendPush>> pushList() {
        return Result.success(service.pushList());
    }

    @Operation(summary = "新建人工推送")
    @PostMapping("/push")
    public Result<RecommendPush> createPush(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @Valid @RequestBody RecommendPushRequest request) {
        return Result.success("推送已创建", service.savePush(null, request, adminId));
    }

    @Operation(summary = "更新人工推送")
    @PutMapping("/push/{id}")
    public Result<RecommendPush> updatePush(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @Valid @RequestBody RecommendPushRequest request) {
        return Result.success("推送已更新", service.savePush(id, request, adminId));
    }

    @Operation(summary = "启用、暂停或结束人工推送")
    @PostMapping("/push/{id}/status")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        service.changeStatus(id, status);
        return Result.success("状态已更新", null);
    }

    @Operation(summary = "人工推送效果")
    @GetMapping("/metrics")
    public Result<List<Map<String, Object>>> metrics() {
        return Result.success(service.pushMetrics());
    }
}
