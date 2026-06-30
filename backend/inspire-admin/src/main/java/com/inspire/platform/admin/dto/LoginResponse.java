package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor @Schema(description = "管理员登录响应")
public class LoginResponse {
    @Schema(description = "JWT令牌") private String token;
    @Schema(description = "管理员ID") private Integer id;
    @Schema(description = "管理员账号") private String username;
    @Schema(description = "管理员昵称") private String nickname;
}
