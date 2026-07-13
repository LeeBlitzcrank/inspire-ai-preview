package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录 / 刷新 成功后返回的双Token响应
 * <p>
 * 文档 4.1.2 节返回结构规范：
 * <pre>{@code {"accessToken":"...","refreshToken":"...","expiresIn":900}}</pre>
 */
@Schema(description = "登录/刷新成功返回的双Token及用户信息")
public class TokenResponse {

    @Schema(description = "AccessToken（JWT短期业务令牌），有效期15分钟，请求头 Authorization: Bearer {token}")
    private String accessToken;

    @Schema(description = "RefreshToken（32位随机字符串），有效期7天，刷新/登出时使用")
    private String refreshToken;

    @Schema(description = "AccessToken过期秒数", example = "900")
    private Long expiresIn;

    @Schema(description = "用户ID", example = "196312085385187329")
    private Long userId;

    @Schema(description = "用户名", example = "alice")
    private String username;

    @Schema(description = "用户昵称", example = "快乐小鱼")
    private String nickname;

    @Schema(description = "头像URL", example = "🌸")
    private String avatar;

    @Schema(description = "角色: admin/core/user", example = "user")
    private String role;
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String v) { this.accessToken = v; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String v) { this.refreshToken = v; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long v) { this.expiresIn = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String v) { this.avatar = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }

}
