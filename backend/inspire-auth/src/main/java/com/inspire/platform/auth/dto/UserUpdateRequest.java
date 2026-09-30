/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/dto/UserUpdateRequest.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
