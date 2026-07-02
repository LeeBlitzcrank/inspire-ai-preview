package com.inspire.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.auth.dto.*;
import com.inspire.platform.auth.entity.PasswordReset;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.mapper.PasswordResetMapper;
import com.inspire.platform.auth.mapper.UserMapper;
import com.inspire.platform.auth.service.AuthService;
import com.inspire.platform.auth.service.email.EmailService;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordResetMapper passwordResetMapper;
    private final EmailService emailService;
    private final MqProducer mqProducer;
    private final SecretKey secretKey;
    private final long expirationMs;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordResetMapper passwordResetMapper,
                           EmailService emailService,
                           MqProducer mqProducer,
                           @Value("${inspire.jwt.secret}") String secret,
                           @Value("${inspire.jwt.expiration:604800000}") long expiration) {
        this.userMapper = userMapper;
        this.passwordResetMapper = passwordResetMapper;
        this.emailService = emailService;
        this.mqProducer = mqProducer;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    // ==================== 注册 ====================

    @Override
    public TokenResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }
        if (findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已被注册");
        }
        if (findByEmail(request.getEmail()) != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        User user = new User();
        user.setId(nextId());
        user.setUsername(request.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null && !request.getNickname().isEmpty() ? request.getNickname() : request.getUsername());
        user.setAvatar("");
        user.setCity("");
        user.setDeleted(0);

        userMapper.insert(user);
        mqProducer.send(MqTopicConstants.TOPIC_USER_REGISTER, java.util.Map.of("userId", user.getId(), "username", user.getUsername(), "email", user.getEmail()));
        log.info("用户注册成功: userId={}, username={}, email={}", user.getId(), user.getUsername(), user.getEmail());
        return buildTokenResponse(user);
    }

    // ==================== 登录 ====================

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        mqProducer.send(MqTopicConstants.TOPIC_USER_BEHAVIOR, java.util.Map.of("userId", user.getId(), "username", user.getUsername(), "type", "login"));
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResponse(user);
    }

    // ==================== 个人信息 ====================

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public User updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);
        if (request.getNickname() != null) user.setNickname(request.getNickname().isEmpty() ? user.getUsername() : request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getCity() != null) user.setCity(request.getCity());
        userMapper.updateById(user);
        log.info("用户信息更新: userId={}", userId);
        return user;
    }

    // ==================== 修改密码 ====================

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!PASSWORD_ENCODER.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(PASSWORD_ENCODER.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("密码修改成功: userId={}", userId);
    }

    // ==================== 退出登录 ====================

    @Override
    public void logout(Long userId) {
        log.info("用户退出登录: userId={}", userId);
    }

    // ==================== 忘记密码 ====================

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = findByEmail(email);
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        // 生成UUID令牌，30分钟过期
        String token = UUID.randomUUID().toString().replace("-", "");
        PasswordReset record = new PasswordReset();
        record.setId(nextId());
        record.setUserId(user.getId());
        record.setEmail(email);
        record.setToken(token);
        record.setExpiryTime(LocalDateTime.now().plusMinutes(30));
        record.setUsed(0);

        passwordResetMapper.insert(record);

        // 发送邮件（SMTP或控制台兜底）
        emailService.sendPasswordResetEmail(email, token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LambdaQueryWrapper<PasswordReset> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PasswordReset::getToken, token);
        wrapper.eq(PasswordReset::getUsed, 0);
        PasswordReset record = passwordResetMapper.selectOne(wrapper);

        if (record == null) {
            throw new BusinessException("重置令牌无效或已使用");
        }
        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置令牌已过期，请重新申请");
        }

        // 更新密码
        User user = getUserById(record.getUserId());
        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
        userMapper.updateById(user);

        // 标记令牌已使用
        record.setUsed(1);
        passwordResetMapper.updateById(record);

        log.info("密码重置成功: userId={}", user.getId());
    }

    // ==================== 查询工具 ====================

    private User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    private User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getEmail, email);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    // ==================== JWT ====================

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

    // ==================== 雪花ID ====================

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
            if (sequence == 0) timestamp++;
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - 1735689600000L) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << WORKER_SHIFT) | WORKER_ID;
    }
}
