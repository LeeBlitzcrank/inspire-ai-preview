package com.inspire.platform.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具类（Auth 服务 —— 签发端）
 * <p>
 * 文档 4.1.2 节规范：
 * — 加密算法：HS256
 * — 密钥：环境变量 INSPIRE_JWT_SECRET 注入
 * — AccessToken 有效期：15 分钟（900 秒）
 * — 载荷仅存放非敏感标识：sub(userId)、userName、role
 * <p>
 * 区分于网关的 JwtUtil：本类提供 generate 方法，网关侧仅做校验。
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * @param secret     Base64 编码的 HMAC-SHA 密钥，≥256 位（32 字节）
     * @param expiration AccessToken 过期毫秒数，默认 900000（15 分钟）
     */
    public JwtUtil(
            @Value("${inspire.jwt.secret}") String secret,
            @Value("${inspire.jwt.access-token-expiration:900000}") long expiration) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            log.warn("JWT密钥非Base64格式，使用UTF-8字节降级（仅开发环境使用）");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            log.error("JWT密钥不足256位（当前{}位），请设置环境变量 INSPIRE_JWT_SECRET", keyBytes.length * 8);
            throw new IllegalArgumentException("JWT密钥长度不足256位，请配置安全密钥");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
        log.info("JwtUtil初始化完成: 密钥长度={}位, 有效期={}ms", keyBytes.length * 8, expiration);
    }

    /**
     * 生成 AccessToken（JWT）
     * <p>
     * 文档 4.1.2 载荷规范：
     * <pre>
     * {
     *   "sub": "userId",
     *   "userName": "用户名",
     *   "role": "admin/core/user",
     *   "iat": 签发时间戳,
     *   "exp": 过期时间戳
     * }
     * </pre>
     *
     * @param userId   用户ID
     * @param userName 用户名
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String userName, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userName", userName)
                .claim("role", role != null ? role : "user")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT（签名 + 过期）
     * <p>
     * 调用方通过异常类型区分错误：
     * — ExpiredJwtException → 令牌过期（签名为合法）
     * — 其他 JwtException   → 签名篡改/格式非法
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws ExpiredJwtException 令牌已过期
     * @throws JwtException        签名非法
     */
    public Claims parseToken(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 计算 JWT 剩余存活秒数（用于黑名单 TTL）
     *
     * @param token JWT 字符串
     * @return 剩余秒数，最小 0
     */
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            long diffMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(0, diffMs / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (JwtException e) {
            return 0;
        }
    }

    /**
     * 从 JWT 中提取 userId（不校验过期）
     *
     * @param token JWT 字符串
     * @return userId，解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(parseToken(token).getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 AccessToken 有效期（毫秒）
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
