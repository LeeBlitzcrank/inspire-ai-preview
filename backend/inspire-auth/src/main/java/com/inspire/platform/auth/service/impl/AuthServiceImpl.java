package com.inspire.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.auth.dto.LoginRequest;
import com.inspire.platform.auth.dto.RegisterRequest;
import com.inspire.platform.auth.dto.TokenResponse;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.mapper.UserMapper;
import com.inspire.platform.auth.service.AuthService;
import com.inspire.platform.common.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final SecretKey secretKey;
    private final long expirationMs;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper,
                           @Value("${inspire.jwt.secret}") String secret,
                           @Value("${inspire.jwt.expiration:604800000}") long expiration) {
        this.userMapper = userMapper;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    @Override
    public TokenResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        User exist = findByUsername(request.getUsername());
        if (exist != null) {
            throw new BusinessException("用户名已被注册");
        }

        User user = new User();
        user.setId(nextId());
        user.setUsername(request.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setAvatar("");
        user.setCity("");
        user.setDeleted(0);

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResponse(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResponse(user);
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    private String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    private TokenResponse buildTokenResponse(User user) {
        String token = generateToken(user.getId(), user.getUsername());
        return new TokenResponse(token, user.getId(), user.getUsername());
    }

    // ========== 简易雪花ID ==========
    private static final long WORKER_ID = 1L;
    private static final long DATACENTER_ID = 1L;
    private static final long SEQUENCE_MASK = 0xFFF;
    private static final long TIMESTAMP_SHIFT = 22;
    private static final long WORKER_SHIFT = 12;
    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    private static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) timestamp = lastTimestamp;
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) { timestamp++; }
        } else { sequence = 0; }
        lastTimestamp = timestamp;
        return ((timestamp - 1735689600000L) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << WORKER_SHIFT) | WORKER_ID;
    }
}
