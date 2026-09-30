/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/ResetPasswordRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "重置密码请求（通过重置令牌设置新密码）")
public class ResetPasswordRequest {

    @Schema(description = "重置令牌（通过忘记密码接口发送到邮箱）", example = "a1b2c3d4e5f6...")
    @NotBlank(message = "重置令牌不能为空")
    private String token;

    @Schema(description = "新密码，6-16位", example = "newpass789")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6-16位")
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { this.newPassword = v; }
}
