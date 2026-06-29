package com.inspire.platform.auth.service;

import com.inspire.platform.auth.dto.LoginRequest;
import com.inspire.platform.auth.dto.RegisterRequest;
import com.inspire.platform.auth.dto.TokenResponse;
import com.inspire.platform.auth.entity.User;

public interface AuthService {

    /**
     * 用户注册
     * PRD 3.模块1：用户名唯一、密码长度6-16位、BCrypt加密
     */
    TokenResponse register(RegisterRequest request);

    /**
     * 用户登录
     * PRD 3.模块1：校验密码、下发JWT
     */
    TokenResponse login(LoginRequest request);

    /**
     * 根据用户ID查询用户信息
     */
    User getUserById(Long userId);
}
