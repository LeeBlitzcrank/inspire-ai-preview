package com.inspire.platform.admin.service;
import com.inspire.platform.admin.dto.LoginResponse;
public interface AdminAuthService {
    LoginResponse login(String username, String password);
}
