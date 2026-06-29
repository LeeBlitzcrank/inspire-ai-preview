package com.inspire.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("password_reset")
public class PasswordReset {
    private Long id;
    private Long userId;
    private String email;
    private String token;
    private LocalDateTime expiryTime;
    private Integer used;
    private LocalDateTime createTime;
}
