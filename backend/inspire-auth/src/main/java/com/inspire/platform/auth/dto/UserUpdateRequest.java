package com.inspire.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateRequest {

    @Schema(description = "用户昵称", example = "Alice魔法师")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "所在城市", example = "上海")
    private String city;
}
