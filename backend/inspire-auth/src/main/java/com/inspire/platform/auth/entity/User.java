package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户主表 entity
 * 对应 PRD sqlDoc 中 user 表
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String username;

    private String password;

    @TableField(fill = FieldFill.INSERT)
    private String avatar;

    @TableField(fill = FieldFill.INSERT)
    private String nickname;

    private String city;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    private String extJson;
}
