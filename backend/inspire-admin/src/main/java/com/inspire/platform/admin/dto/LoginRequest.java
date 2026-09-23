package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data @Schema(description = "管理员登录请求")
public class LoginRequest {
    @Schema(description = "管理员账号", example = "admin")
    @NotBlank private String username;
    @Schema(description = "密码", example = "112233")
    @NotBlank private String password;
    @Schema(description = "TOTP 动态验证码")
    private String mfaCode;
}
