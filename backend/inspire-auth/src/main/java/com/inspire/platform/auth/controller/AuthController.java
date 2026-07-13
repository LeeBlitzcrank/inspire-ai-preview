package com.inspire.platform.auth.controller;

import com.inspire.platform.auth.dto.*;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.service.AuthService;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户认证", description = "用户注册、登录、个人信息管理、密码管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册", description = "注册新账号，自动登录并返回双Token（accessToken + refreshToken）")
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", authService.register(request));
    }

    @Operation(summary = "用户登录", description = "文档流程一：账号密码登录，返回双Token + 用户信息，单点登录挤旧设备")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    // ==================== 无感刷新（文档流程三） ====================

    @Operation(summary = "刷新AccessToken", description = "文档流程三：携带RefreshToken获取新AccessToken，RefreshToken不更新，7天内可重复使用")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(
            @Parameter(description = "RefreshToken（32位随机字符串，有效期7天）")
            @RequestHeader("Refresh-Token") String refreshToken) {
        return Result.success("刷新成功", authService.refreshToken(refreshToken));
    }

    // ==================== 用户登出（文档流程四） ====================

    @Operation(summary = "用户登出", description = "文档流程四：同时传入AccessToken和RefreshToken，写入黑名单 + 清除会话缓存")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "Authorization头：Bearer {accessToken}")
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = (body != null) ? body.get("refreshToken") : null;
        authService.logout(accessToken, refreshToken);
        return Result.success("已退出登录", null);
    }

    // ==================== 强制下线（文档流程五） ====================

    @Operation(summary = "强制用户下线（管理员）", description = "文档流程五：管理员根据userId踢人，清除该用户全部会话缓存，需admin角色")
    @PostMapping("/admin/kick/{userId}")
    public Result<Void> kickUser(
            @Parameter(description = "操作管理员ID（由网关从JWT解析注入）")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "操作管理员角色（必须为admin）")
            @RequestHeader("X-User-Role") String adminRole,
            @Parameter(description = "被踢下线的用户ID")
            @PathVariable Long userId) {
        if (!"admin".equalsIgnoreCase(adminRole)) {
            log.warn("踢人失败: 非管理员操作 adminUserId={}, role={}", adminUserId, adminRole);
            throw new BusinessException(403001, "当前角色无接口访问权限");
        }
        authService.kickUser(adminUserId, userId);
        return Result.success("强制下线成功", null);
    }

    // ==================== 个人信息 ====================

    @Operation(summary = "获取个人信息", description = "获取当前登录用户的基本信息（不含密码），需在请求头中携带JWT令牌")
    @GetMapping("/userinfo")
    public Result<User> userinfo(
            @Parameter(description = "用户ID（由网关从JWT中解析并注入，无需手动传递）")
            @RequestHeader("X-User-Id") Long userId) {
        User user = authService.getUserById(userId);
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "公开用户信息", description = "无需Token，按userId查询用户基本信息")
    @GetMapping("/user/public/{id}")
    public Result<User> publicUserInfo(@PathVariable Long id) {
        User user = authService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "修改个人信息", description = "修改昵称、头像、城市等，只传需要修改的字段")
    @PutMapping("/userinfo")
    public Result<User> updateUserinfo(
            @Parameter(description = "用户ID（由网关从JWT中解析并注入）")
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        User user = authService.updateUserInfo(userId, request);
        user.setPassword(null);
        return Result.success("更新成功", user);
    }

    @Operation(summary = "修改密码", description = "验证旧密码后更新为新密码，密码长度6-16位")
    @PutMapping("/password")
    public Result<Void> changePassword(
            @Parameter(description = "用户ID（由网关从JWT中解析并注入）")
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    // ==================== 密码重置 ====================

    @Operation(summary = "忘记密码", description = "通过注册邮箱发送密码重置链接，链接中携带有效期30分钟的令牌")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return Result.success("重置链接已发送至您的邮箱，请查收", null);
    }

    @Operation(summary = "重置密码", description = "使用忘记密码接口获取的令牌设置新密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return Result.success("密码重置成功", null);
    }

    // ==================== IP定位 ====================

    @Operation(summary = "IP定位", description = "获取客户端IP所在城市。支持 X-Forwarded-For 和 ip 查询参数")
    @GetMapping("/ip-location")
    public Result<Map<String, String>> ipLocation(
            HttpServletRequest request,
            @RequestParam(required = false) String ip) {
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        Map<String, String> data = new HashMap<>();
        data.put("query_ip", ip != null ? ip : "");
        System.out.println("[IP定位] 获取到IP: " + ip);

        try {
            RestTemplate rt = new RestTemplate();
            String queryIp = ip;
            Map result = rt.getForObject(
                "http://ip-api.com/json/" + queryIp + "?fields=status,regionName,city", Map.class);
            System.out.println("[IP定位] ip-api返回: " + result);
            if (result != null && "success".equals(result.get("status"))) {
                data.put("region", result.getOrDefault("regionName", "").toString());
                data.put("city", result.getOrDefault("city", "").toString());
            }
        } catch (Exception e) {
            System.out.println("[IP定位] 调用失败: " + e.getMessage());
        }
        // 仍然没有数据时默认北京
        if (!data.containsKey("city") || data.get("city") == null || data.get("city").isEmpty()) {
            data.put("region", "北京");
            data.put("city", "北京");
            data.put("note", "自动定位失败，默认北京");
        }
        return Result.success(data);
    }
}
