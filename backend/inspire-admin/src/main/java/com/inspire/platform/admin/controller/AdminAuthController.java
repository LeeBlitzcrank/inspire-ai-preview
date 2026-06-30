package com.inspire.platform.admin.controller;

import com.inspire.platform.admin.dto.LoginRequest;
import com.inspire.platform.admin.dto.LoginResponse;
import com.inspire.platform.admin.service.AdminAuthService;
import com.inspire.platform.admin.service.AdminDashboardService;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.admin.dto.DashboardVO;
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
        return Result.success(adminAuthService.login(req.getUsername(), req.getPassword()));
    }

    @Operation(summary = "监控大屏", description = "获取平台实时统计数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminDashboardService.getDashboard());
    }
}
