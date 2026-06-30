package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data @Schema(description = "管理员登录请求")
public class LoginRequest {
    @Schema(description = "管理员账号", example = "admin")
    @NotBlank private String username;
    @Schema(description = "密码", example = "admin123")
    @NotBlank private String password;
}
