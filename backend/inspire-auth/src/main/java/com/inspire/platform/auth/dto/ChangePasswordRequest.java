package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "旧密码，用于验证身份", example = "123456")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码，6-16位", example = "654321")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6-16位")
    private String newPassword;
}
