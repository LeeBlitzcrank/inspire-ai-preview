/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/controller/ConfigController.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.controller;

import com.inspire.platform.admin.entity.AdminConfig;
import com.inspire.platform.admin.service.AdminConfigService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@Tag(name = "配置推送", description = "推荐权重配置、手动推送")
@RestController @RequestMapping("/admin/config") @RequiredArgsConstructor
public class ConfigController {
    private final AdminConfigService configService;

    @Operation(summary = "配置列表", description = "获取所有运营配置")
    @GetMapping("/list")
    public Result<List<AdminConfig>> list() {
        return Result.success(configService.getAll());
    }

    @Operation(summary = "更新配置", description = "修改配置值")
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        configService.update((Integer) body.get("id"), (String) body.get("value"));
        return Result.success("更新成功", null);
    }

    @Operation(summary = "手动推送", description = "向指定城市用户推送灵感消息")
    @PostMapping("/push")
    public Result<Void> push(@RequestBody Map<String, String> body) {
        configService.push(body.get("title"), body.get("content"), body.get("city"));
        return Result.success("推送已发送", null);
    }
}
