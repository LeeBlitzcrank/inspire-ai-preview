package com.inspire.platform.auth.dto;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "登录/注册成功返回的令牌信息")
public class TokenResponse {

    @Schema(description = "JWT令牌，后续请求需在Authorization头中携带", example = "eyJhbGciOiJIUzM4NCJ9...")
    private String token;

    @Schema(description = "用户ID（雪花ID）", example = "196312085385187329")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "用户名", example = "alice")
    private String username;
}
