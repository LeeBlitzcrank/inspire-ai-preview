/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/LoginRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.inspire.platform.common.validation.ValidationConstants.LOGIN_PASSWORD_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.USERNAME_MAX;

@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "登录用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = USERNAME_MAX, message = "账号长度不正确")
    private String username;

    @Schema(description = "登录密码")
    @NotBlank(message = "密码不能为空")
    @Size(max = LOGIN_PASSWORD_MAX, message = "密码长度不正确")
    private String password;

    @Schema(description = "设备唯一标识")
    @Size(max = 128, message = "设备标识过长")
    private String deviceId;

    @Schema(description = "图形验证码ID")
    @Size(max = 64, message = "验证码ID过长")
    private String captchaId;

    @Schema(description = "图形验证码")
    @Size(max = 10, message = "验证码长度不正确")
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
