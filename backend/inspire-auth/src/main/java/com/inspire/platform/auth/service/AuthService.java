package com.inspire.platform.auth.service;

import com.inspire.platform.auth.dto.*;
import com.inspire.platform.auth.entity.User;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    /**
     * 无感刷新 AccessToken（文档流程三）
     *
     * @param refreshToken 前端携带的 RefreshToken
     * @return 包含新 accessToken 的响应（refreshToken 不更新）
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * 用户主动登出（文档流程四）
     *
     * @param accessToken  前端携带的 AccessToken
     * @param refreshToken 前端携带的 RefreshToken
     */
    void logout(String accessToken, String refreshToken);

    /**
     * 后台管理员强制用户下线（文档流程五）
     *
     * @param adminUserId 操作的管理员用户ID
     * @param targetUserId 被踢下线的用户ID
     */
    void kickUser(Long adminUserId, Long targetUserId);

    User getUserById(Long userId);

    User updateUserInfo(Long userId, UserUpdateRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
