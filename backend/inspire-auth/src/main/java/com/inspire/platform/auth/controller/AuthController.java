package com.inspire.platform.auth.controller;

import com.inspire.platform.auth.dto.LoginRequest;
import com.inspire.platform.auth.dto.RegisterRequest;
import com.inspire.platform.auth.dto.TokenResponse;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.service.AuthService;
import com.inspire.platform.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * PRD 3.模块1 注册登录页
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse response = authService.register(request);
        return Result.success("注册成功", response);
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/userinfo
     * 网关鉴权后从请求头 X-Inspire-UserId 获取用户ID
     */
    @GetMapping("/userinfo")
    public Result<User> userinfo(@RequestHeader("X-Inspire-UserId") Long userId) {
        User user = authService.getUserById(userId);
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }
}
