package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor @Schema(description = "管理员登录响应")
public class LoginResponse {
    @Schema(description = "JWT令牌") private String token;
    @Schema(description = "管理员ID") private Integer id;
    @Schema(description = "管理员账号") private String username;
    @Schema(description = "管理员昵称") private String nickname;
    @Schema(description = "是否需要输入 TOTP 验证码") private Boolean mfaRequired;
    @Schema(description = "是否需要先完成 TOTP 绑定") private Boolean mfaSetupRequired;
    @Schema(description = "首次绑定使用的 TOTP 密钥") private String totpSecret;
    @Schema(description = "TOTP otpauth 地址") private String otpauthUri;
}
