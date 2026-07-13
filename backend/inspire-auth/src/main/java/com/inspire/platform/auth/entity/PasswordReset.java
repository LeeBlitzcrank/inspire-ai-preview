package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("password_reset")
public class PasswordReset {
    private Long id;
    private Long userId;
    private String email;
    private String token;
    private LocalDateTime expiryTime;
    private Integer used;
    private LocalDateTime createTime;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime v) { this.expiryTime = v; }
    public Integer getUsed() { return used; }
    public void setUsed(Integer v) { this.used = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }

}
