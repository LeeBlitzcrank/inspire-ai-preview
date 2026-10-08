/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/ForgotPasswordRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.inspire.platform.common.validation.ValidationConstants.EMAIL_MAX;

@Schema(description = "忘记密码请求（发送重置邮件）")
public class ForgotPasswordRequest {

    @Schema(description = "注册时填写的邮箱地址")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = EMAIL_MAX, message = "邮箱长度不能超过254位")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
}
