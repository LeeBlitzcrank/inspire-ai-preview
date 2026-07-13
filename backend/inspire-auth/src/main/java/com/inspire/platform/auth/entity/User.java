package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@TableName("user")
@Schema(description = "用户信息")
public class User {

    @Schema(description = "用户ID（雪花算法生成）")
    @TableId(type = IdType.INPUT)
    private Long id;
    @Schema(description = "登录用户名")
    private String username;
    @Schema(description = "密码（BCrypt加密）", hidden = true)
    private String password;
    @Schema(description = "用户邮箱")
    private String email;
    @Schema(description = "头像URL")
    @TableField(fill = FieldFill.INSERT)
    private String avatar;
    @Schema(description = "用户昵称")
    @TableField(fill = FieldFill.INSERT)
    private String nickname;
    @Schema(description = "角色: admin/core/user")
    private String role;
    @Schema(description = "常居城市")
    private String city;
    @Schema(description = "注册时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @Schema(description = "信息更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @Schema(hidden = true)
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
    @Schema(description = "状态: 1正常 0冻结")
    @TableField(fill = FieldFill.INSERT)
    private Integer status;
    @Schema(description = "扩展字段")
    private String extJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getExtJson() { return extJson; }
    public void setExtJson(String extJson) { this.extJson = extJson; }
}
