package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "登录用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "登录密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "设备唯一标识")
    private String deviceId;

    @Schema(description = "图形验证码ID")
    private String captchaId;

    @Schema(description = "图形验证码")
    private String captchaCode;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String v) { this.deviceId = v; }
    public String getCaptchaId() { return captchaId; }
    public void setCaptchaId(String v) { this.captchaId = v; }
    public String getCaptchaCode() { return captchaCode; }
    public void setCaptchaCode(String v) { this.captchaCode = v; }
}
