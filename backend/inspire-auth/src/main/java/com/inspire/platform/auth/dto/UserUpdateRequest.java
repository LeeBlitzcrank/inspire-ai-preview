package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户信息更新请求")
public class UserUpdateRequest {

    @Schema(description = "用户昵称", example = "Alice魔法师")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "所在城市", example = "上海")
    private String city;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
