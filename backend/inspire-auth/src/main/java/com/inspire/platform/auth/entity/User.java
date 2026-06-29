package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
@Schema(description = "用户信息")
public class User {

    @Schema(description = "用户ID（雪花算法生成）", example = "196312085385187329")
    @TableId(type = IdType.INPUT)
    private Long id;

    @Schema(description = "登录用户名", example = "alice")
    private String username;

    @Schema(description = "密码（BCrypt加密，接口返回时隐藏）", hidden = true)
    private String password;

    @Schema(description = "用户邮箱", example = "alice@example.com")
    private String email;

    @Schema(description = "头像URL")
    @TableField(fill = FieldFill.INSERT)
    private String avatar;

    @Schema(description = "用户昵称", example = "Alice魔法师")
    @TableField(fill = FieldFill.INSERT)
    private String nickname;

    @Schema(description = "常居城市", example = "上海")
    private String city;

    @Schema(description = "注册时间", example = "2026-06-27 10:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "信息更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(hidden = true)
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @Schema(description = "扩展字段（JSON格式，预留）")
    private String extJson;
}
