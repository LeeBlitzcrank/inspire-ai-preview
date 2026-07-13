package com.inspire.platform.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 令牌工具类
 * - 生成令牌（登录成功时下发）
 * - 校验令牌（网关过滤器调用）
 * - 从令牌中提取用户信息
 *
 * PRD 2.1 登录鉴权规则：JWT令牌，存入Redis缓存，有效期7天
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * 构造时从配置读取密钥和过期时间
     * @param secret BASE64编码的HMAC密钥（至少256位，即32字节）
     * @param expiration 过期毫秒数，默认7天
     */
    public JwtUtil(
            @Value("${inspire.jwt.secret}") String secret,
            @Value("${inspire.jwt.expiration:604800000}") long expiration) {
        // 如果密钥不足256位，自动补足
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    /**
     * 生成JWT令牌
     *
     * @param userId   用户ID（sub）
     * @param username 用户名（自定义claim）
     * @return JWT字符串
     */
    public String generateToken(Long userId, String username) {
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

    /**
     * 校验令牌并解析Claims
     *
     * @param token JWT字符串
     * @return 解析成功返回Claims，失败返回null
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT格式异常: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT签名校验失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT参数异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从令牌中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }

    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("username", String.class);
        }
        return null;
    }

    /**
     * 判断令牌是否已过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }
}
