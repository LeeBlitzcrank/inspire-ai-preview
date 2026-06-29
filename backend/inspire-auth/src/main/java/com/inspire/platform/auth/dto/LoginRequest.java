package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "登录用户名", example = "alice")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
