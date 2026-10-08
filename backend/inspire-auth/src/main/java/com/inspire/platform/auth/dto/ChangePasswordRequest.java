/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/ChangePasswordRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "旧密码，用于验证身份", example = "OldPass123")
    @NotBlank(message = "旧密码不能为空")
    @Size(max = LOGIN_PASSWORD_MAX, message = "旧密码长度不正确")
    private String oldPassword;

    @Schema(description = "新密码，8-64位", example = "NewPass123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = "密码长度需为8-64位")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
