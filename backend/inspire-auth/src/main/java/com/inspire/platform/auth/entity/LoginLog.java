package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 登录操作日志
 * <p>
 * 文档 6.2 节要求：记录IP、设备、时间、操作结果。
 * 使用 MyBatis-Plus 持久化至 login_log 表。
 */
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String username;

    /** 客户端IP */
    private String ip;

    /** 设备标识 */
    private String deviceId;

    /** 浏览器/设备UA */
    private String userAgent;

    /** 登录方式: password / refresh */
    private String loginType;

    /** 结果: 1成功 0失败 */
    private Integer result;

    /** 失败原因 */
    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String v) { this.deviceId = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getLoginType() { return loginType; }
    public void setLoginType(String v) { this.loginType = v; }
    public Integer getResult() { return result; }
    public void setResult(Integer v) { this.result = v; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String v) { this.failReason = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }

}
