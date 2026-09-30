/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/controller/AdminAuthController.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.controller;

import com.inspire.platform.admin.dto.DashboardVO;
import com.inspire.platform.admin.dto.LoginRequest;
import com.inspire.platform.admin.dto.LoginResponse;
import com.inspire.platform.admin.service.AdminAuthService;
import com.inspire.platform.admin.service.AdminDashboardService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员后台", description = "登录、监控大屏")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "管理员登录", description = "使用管理员账号密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(adminAuthService.login(
                req.getUsername(), req.getPassword(), req.getMfaCode()));
    }

    @Operation(summary = "监控大屏", description = "获取平台实时统计数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminDashboardService.getDashboard());
    }
}
