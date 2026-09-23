package com.inspire.platform.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.dto.LoginResponse;
import com.inspire.platform.admin.entity.AdminUser;
import com.inspire.platform.admin.mapper.AdminUserMapper;
import com.inspire.platform.admin.service.AdminAuthService;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.util.TotpUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String FAIL_PREFIX = "admin_login_fail:";
    private static final String LOCK_PREFIX = "admin_login_lock:";
    private static final String TOTP_STEP_PREFIX = "admin_totp_step:";
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final AdminUserMapper adminUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final SecretKey secretKey;
    private final long expirationMs;

    @Value("${inspire.admin-mfa.lock-after-failures:5}")
    private int lockAfterFailures;

    @Value("${inspire.admin-mfa.failure-window-seconds:900}")
    private long failureWindowSeconds;

    @Value("${inspire.admin-mfa.lock-seconds:1800}")
    private long lockSeconds;

    public AdminAuthServiceImpl(AdminUserMapper adminUserMapper,
                                StringRedisTemplate redisTemplate,
                                @Value("${inspire.jwt.secret}") String secret,
                                @Value("${inspire.jwt.expiration:315360000000}") long expiration) {
        this.adminUserMapper = adminUserMapper;
        this.redisTemplate = redisTemplate;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    @PostConstruct
    public void init() {
        long count = adminUserMapper.selectCount(Wrappers.emptyWrapper());
        if (count == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(PASSWORD_ENCODER.encode("112233"));
            admin.setNickname("超级管理员");
            admin.setTotpEnabled(0);
            adminUserMapper.insert(admin);
            log.info("默认管理员已创建: admin / 112233，首次登录需要绑定 TOTP");
        }
    }

    @Override
    public LoginResponse login(String username, String password, String mfaCode) {
        String key = normalizedKey(username);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + key))) {
            throw new BusinessException(429, "管理员登录失败次数过多，账号已临时锁定 30 分钟");
        }

        AdminUser user = adminUserMapper.selectOne(
                Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getUsername, username));
        if (user == null || !PASSWORD_ENCODER.matches(password, user.getPassword())) {
            recordFailure(key);
            throw new BusinessException(400, "管理员账号或密码错误");
        }

        boolean enabled = Integer.valueOf(1).equals(user.getTotpEnabled());
        if (!enabled) {
            if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
                user.setTotpSecret(TotpUtil.generateSecret());
                user.setTotpEnabled(0);
                adminUserMapper.updateById(user);
            }
            if (mfaCode == null || mfaCode.isBlank()) {
                return setupChallenge(user);
            }
            long step = verifyTotp(user, mfaCode);
            if (step < 0) {
                recordFailure(key);
                throw new BusinessException(400, "动态验证码错误");
            }
            user.setTotpEnabled(1);
            adminUserMapper.updateById(user);
            clearFailures(key);
            return tokenResponse(user);
        }

        if (mfaCode == null || mfaCode.isBlank()) {
            LoginResponse response = new LoginResponse();
            response.setMfaRequired(true);
            response.setUsername(user.getUsername());
            return response;
        }
        long step = verifyTotp(user, mfaCode);
        if (step < 0) {
            recordFailure(key);
            throw new BusinessException(400, "动态验证码错误");
        }
        clearFailures(key);
        return tokenResponse(user);
    }

    private LoginResponse setupChallenge(AdminUser user) {
        LoginResponse response = new LoginResponse();
        response.setMfaRequired(true);
        response.setMfaSetupRequired(true);
        response.setUsername(user.getUsername());
        response.setTotpSecret(user.getTotpSecret());
        response.setOtpauthUri(buildOtpAuthUri(user));
        return response;
    }

    private LoginResponse tokenResponse(AdminUser user) {
        LoginResponse response = new LoginResponse();
        response.setToken(generateToken(user));
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setMfaRequired(false);
        response.setMfaSetupRequired(false);
        return response;
    }

    private long verifyTotp(AdminUser user, String code) {
        long step = TotpUtil.verifyAndGetStep(user.getTotpSecret(), code);
        if (step < 0) return -1L;
        String replayKey = TOTP_STEP_PREFIX + user.getId() + ":" + step;
        Boolean accepted = redisTemplate.opsForValue().setIfAbsent(
                replayKey, "1", Duration.ofSeconds(90));
        return Boolean.TRUE.equals(accepted) ? step : -1L;
    }

    private String buildOtpAuthUri(AdminUser user) {
        String label = URLEncoder.encode("Inspire Admin:" + user.getUsername(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "otpauth://totp/" + label
                + "?secret=" + user.getTotpSecret()
                + "&issuer=" + URLEncoder.encode("Inspire Admin", StandardCharsets.UTF_8);
    }

    private void recordFailure(String key) {
        Long failures = redisTemplate.opsForValue().increment(FAIL_PREFIX + key);
        redisTemplate.expire(FAIL_PREFIX + key, Duration.ofSeconds(failureWindowSeconds));
        if (failures != null && failures >= lockAfterFailures) {
            redisTemplate.opsForValue().set(
                    LOCK_PREFIX + key, "1", Duration.ofSeconds(lockSeconds));
            redisTemplate.delete(FAIL_PREFIX + key);
            throw new BusinessException(429, "管理员登录失败次数过多，账号已临时锁定 30 分钟");
        }
    }

    private void clearFailures(String key) {
        redisTemplate.delete(FAIL_PREFIX + key);
        redisTemplate.delete(LOCK_PREFIX + key);
    }

    private String normalizedKey(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String generateToken(AdminUser user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", "admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
