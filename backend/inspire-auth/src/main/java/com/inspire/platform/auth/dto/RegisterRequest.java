/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/RegisterRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "登录用户名")
    @NotBlank(message = "账号不能为空")
    @Size(min = USERNAME_MIN, max = USERNAME_MAX, message = "账号长度需为4-20位")
    @Pattern(regexp = USERNAME_PATTERN, message = "账号只能包含字母、数字和下划线")
    private String username;
    @Schema(description = "登录密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = "密码长度需为8-64位")
    private String password;
    @Schema(description = "用户邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = EMAIL_MAX, message = "邮箱长度不能超过254位")
    @Email(message = "邮箱格式不正确")
    private String email;
    @Schema(description = "确认密码")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    @Schema(description = "随机昵称")
    @Size(max = NICKNAME_MAX, message = "昵称不能超过20个字符")
    private String nickname;
    @Schema(description = "头像emoji")
    @Size(max = AVATAR_MAX, message = "头像地址过长")
    private String avatar;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String v) { this.confirmPassword = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String v) { this.avatar = v; }
}
