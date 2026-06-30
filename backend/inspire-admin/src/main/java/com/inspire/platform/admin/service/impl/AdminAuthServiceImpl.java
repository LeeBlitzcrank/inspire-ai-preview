package com.inspire.platform.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.dto.LoginResponse;
import com.inspire.platform.admin.entity.AdminUser;
import com.inspire.platform.admin.mapper.AdminUserMapper;
import com.inspire.platform.admin.service.AdminAuthService;
import com.inspire.platform.common.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final SecretKey secretKey;
    private final long expirationMs;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public AdminAuthServiceImpl(AdminUserMapper adminUserMapper,
                                @Value("${inspire.jwt.secret}") String secret,
                                @Value("${inspire.jwt.expiration:604800000}") long expiration) {
        this.adminUserMapper = adminUserMapper;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    @PostConstruct
    public void init() {
        // 首次启动自动创建默认管理员
        long count = adminUserMapper.selectCount(Wrappers.emptyWrapper());
        if (count == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(PASSWORD_ENCODER.encode("admin123"));
            admin.setNickname("超级管理员");
            adminUserMapper.insert(admin);
            log.info("默认管理员已创建: admin / admin123");
        }
    }

    @Override
    public LoginResponse login(String username, String password) {
        AdminUser user = adminUserMapper.selectOne(
                Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getUsername, username));
        if (user == null || !PASSWORD_ENCODER.matches(password, user.getPassword())) {
            throw new BusinessException("管理员账号或密码错误");
        }
        return new LoginResponse(generateToken(user), user.getId(), user.getUsername(), user.getNickname());
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
