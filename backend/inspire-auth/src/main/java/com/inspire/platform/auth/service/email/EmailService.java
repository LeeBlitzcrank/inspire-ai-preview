/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/service/email/EmailService.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.service.email;

public interface EmailService {

    /**
     * 发送密码重置邮件
     * @param to 收件人邮箱
     * @param token 重置令牌
     */
    void sendPasswordResetEmail(String to, String token);
}
