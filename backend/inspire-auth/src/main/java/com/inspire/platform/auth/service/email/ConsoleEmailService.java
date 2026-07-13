package com.inspire.platform.auth.service.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 * - 若配置了 spring.mail.host，则使用 SMTP 真实发送
 * - 否则仅打印日志到控制台（方便本地开发调试）
 */
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${inspire.app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "【灵思集】密码重置通知";
        String text = """
            <html><body>
            <h2>灵思集 - 密码重置</h2>
            <p>您好，请点击以下链接重置密码（链接有效期为30分钟）：</p>
            <p><a href="%s">%s</a></p>
            <p>如果这不是您本人操作，请忽略此邮件。</p>
            </body></html>
            """.formatted(resetLink, resetLink);

        if (mailSender != null) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setTo(to);
                helper.setFrom(mailUsername);
                helper.setSubject(subject);
                helper.setText(text, true);
                mailSender.send(msg);
                log.info("重置密码邮件已发送至: {}", to);
                return;
            } catch (Exception e) {
                log.warn("SMTP发送失败，降级为控制台输出: {}", e.getMessage());
            }
        }

        // 控制台兜底
        log.info("==============================================");
        log.info("收件人: {}", to);
        log.info("主题: {}", subject);
        log.info("重置链接: {}", resetLink);
        log.info("==============================================");
    }
}
