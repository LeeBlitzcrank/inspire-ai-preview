package com.inspire.platform.auth.service;

import com.inspire.platform.auth.dto.*;
import com.inspire.platform.auth.entity.User;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    User getUserById(Long userId);

    User updateUserInfo(Long userId, UserUpdateRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void logout(Long userId);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
