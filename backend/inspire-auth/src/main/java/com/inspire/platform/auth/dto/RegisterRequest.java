package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "登录用户名", example = "alice")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度2-50位")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6-16位")
    private String password;

    @Schema(description = "用户邮箱", example = "alice@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "确认密码，需与密码一致", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "随机昵称（可不传，后端自动生成）", example = "快乐小鱼")
    private String nickname;

    @Schema(description = "头像emoji（可不传，后端自动生成）", example = "🌸")
    private String avatar;
}
