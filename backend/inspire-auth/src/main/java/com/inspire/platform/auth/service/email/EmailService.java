package com.inspire.platform.auth.service.email;

public interface EmailService {

    /**
     * 发送密码重置邮件
     * @param to 收件人邮箱
     * @param token 重置令牌
     */
    void sendPasswordResetEmail(String to, String token);
}
