package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码请求（通过重置令牌设置新密码）")
public class ResetPasswordRequest {

    @Schema(description = "重置令牌（通过忘记密码接口发送到邮箱）", example = "a1b2c3d4e5f6...")
    @NotBlank(message = "重置令牌不能为空")
    private String token;

    @Schema(description = "新密码，6-16位", example = "newpass789")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6-16位")
    private String newPassword;
}
