package com.inspire.platform.auth.service;

import com.inspire.platform.auth.dto.SmsSendResponse;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.validation.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_PREFIX = "sms_code:";
    private static final String COOLDOWN_PREFIX = "sms_cooldown:";
    private static final String DAILY_PREFIX = "sms_daily:";
    private static final String ATTEMPT_PREFIX = "sms_attempt:";

    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;

    @Value("${inspire.sms.code-expire-seconds:300}")
    private long codeExpireSeconds;

    @Value("${inspire.sms.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${inspire.sms.daily-limit:10}")
    private int dailyLimit;

    @Value("${inspire.sms.max-attempts:5}")
    private int maxAttempts;

    @Value("${inspire.sms.dev-return-code:false}")
    private boolean devReturnCode;

    public SmsSendResponse send(String rawPhone, String purpose) {
        String phone = InputValidation.normalizePhone(rawPhone);
        String cooldownKey = COOLDOWN_PREFIX + purpose + ":" + hash(phone);
        Boolean accepted = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));
        if (!Boolean.TRUE.equals(accepted)) {
            Long remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long seconds = remaining == null || remaining <= 0 ? cooldownSeconds : remaining;
            throw new BusinessException(429, "验证码发送过于频繁，请 " + seconds + " 秒后再试");
        }

        String dailyKey = DAILY_PREFIX + LocalDate.now() + ":" + hash(phone);
        Long current = redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofHours(26));
        if (current != null && current > dailyLimit) {
            redisTemplate.delete(cooldownKey);
            throw new BusinessException(429, "今日验证码发送次数已达上限");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String codeKey = codeKey(phone, purpose);
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(codeExpireSeconds));
        redisTemplate.delete(attemptKey(phone, purpose));
        try {
            smsSender.send(phone, code, purpose);
        } catch (RuntimeException e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            throw e;
        }
        return new SmsSendResponse((int) cooldownSeconds, devReturnCode ? code : null);
    }

    public void verify(String rawPhone, String purpose, String rawCode) {
        String phone = InputValidation.normalizePhone(rawPhone);
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("\\d{6}")) {
            throw new BusinessException(400, "验证码必须为6位数字");
        }
        String key = codeKey(phone, purpose);
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }

        String attemptKey = attemptKey(phone, purpose);
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, Duration.ofSeconds(codeExpireSeconds));
        if (attempts != null && attempts > maxAttempts) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            throw new BusinessException(429, "验证码错误次数过多，请重新获取");
        }
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                code.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(400, "验证码错误");
        }
        redisTemplate.delete(key);
        redisTemplate.delete(attemptKey);
    }

    private String codeKey(String phone, String purpose) {
        return CODE_PREFIX + purpose + ":" + hash(phone);
    }

    private String attemptKey(String phone, String purpose) {
        return ATTEMPT_PREFIX + purpose + ":" + hash(phone);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("短信验证码键生成失败", e);
        }
    }
}
