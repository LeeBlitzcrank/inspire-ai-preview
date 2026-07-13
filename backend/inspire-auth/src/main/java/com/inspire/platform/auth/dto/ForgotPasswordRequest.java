package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "忘记密码请求（发送重置邮件）")
public class ForgotPasswordRequest {

    @Schema(description = "注册时填写的邮箱地址")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
}
