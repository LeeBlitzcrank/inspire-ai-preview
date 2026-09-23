package com.inspire.platform.auth.service;

import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 账号级登录失败计数、临时锁定和验证码。
 */
@Service
@RequiredArgsConstructor
public class LoginRiskService {

    private static final String FAIL_PREFIX = "login_fail:";
    private static final String LOCK_PREFIX = "login_lock:";
    private static final String CAPTCHA_PREFIX = "login_captcha:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private final StringRedisTemplate redisTemplate;

    @Value("${inspire.auth.risk.failure-window-seconds:900}")
    private long failureWindowSeconds;

    @Value("${inspire.auth.risk.captcha-after-failures:3}")
    private int captchaAfterFailures;

    @Value("${inspire.auth.risk.lock-after-failures:5}")
    private int lockAfterFailures;

    @Value("${inspire.auth.risk.lock-seconds:1800}")
    private long lockSeconds;

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(username)));
    }

    public boolean requiresCaptcha(String username) {
        String value = redisTemplate.opsForValue().get(failKey(username));
        return value != null && Integer.parseInt(value) >= captchaAfterFailures;
    }

    public void verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank()
                || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        String key = CAPTCHA_PREFIX + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (expected == null || !expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
    }

    public CaptchaResult createCaptcha() {
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            code.append(CAPTCHA_CHARS[RANDOM.nextInt(CAPTCHA_CHARS.length)]);
        }
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String text = code.toString();
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId, text, Duration.ofMinutes(5));

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="132" height="44" viewBox="0 0 132 44">
                  <rect width="132" height="44" rx="10" fill="#f4faf8"/>
                  <path d="M8 31 L30 11 M41 35 L66 9 M75 34 L104 12 M112 30 L126 15"
                        stroke="#b8d8d2" stroke-width="2" opacity="0.75"/>
                  <text x="66" y="30" text-anchor="middle" fill="#295a53"
                        font-family="Arial, sans-serif" font-size="24" font-weight="700"
                        letter-spacing="5">%s</text>
                </svg>
                """.formatted(text);
        String dataUrl = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaResult(captchaId, dataUrl);
    }

    public void recordFailure(String username) {
        String key = failKey(username);
        Long failures = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(failureWindowSeconds));
        if (failures != null && failures >= lockAfterFailures) {
            redisTemplate.opsForValue().set(
                    lockKey(username), "1", Duration.ofSeconds(lockSeconds));
            redisTemplate.delete(key);
            throw new BusinessException(429, "登录失败次数过多，账号已临时锁定 30 分钟");
        }
    }

    public void clearFailures(String username) {
        redisTemplate.delete(failKey(username));
        redisTemplate.delete(lockKey(username));
    }

    private String failKey(String username) {
        return FAIL_PREFIX + hash(normalize(username));
    }

    private String lockKey(String username) {
        return LOCK_PREFIX + hash(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("登录风控键生成失败", e);
        }
    }

    public record CaptchaResult(String captchaId, String image) {
    }
}
