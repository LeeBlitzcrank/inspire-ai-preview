/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/util/JwtUtil.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：通用工具类，提供可复用且无业务状态的静态能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 统一 JWT 令牌工具（inspire-common）
 * <p>
 * gateway 和 auth 共用此工具，消除双模块重复 JWT 代码。
 * 密钥从环境变量 INSPIRE_JWT_SECRET 注入。
 * parseToken 不吞异常，调用方区分失效原因。
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${inspire.jwt.secret}") String secret,
            @Value("${inspire.jwt.access-token-expiration:900000}") long expiration) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            log.warn("JWT密钥非Base64格式，使用UTF-8字节降级");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
    }

    /** 生成 AccessToken（auth 签发用） */
    public String generateToken(Long userId, String userName, String role) {
        return generateToken(userId, userName, role, expirationMs, 0L);
    }

    /** 生成指定有效期的 AccessToken（白名单测试账号支持长期会话）。 */
    public String generateToken(Long userId, String userName, String role, long ttlMs) {
        return generateToken(userId, userName, role, ttlMs, 0L);
    }

    /** 生成带用户令牌版本的 AccessToken，用于改密/重置/踢人后立即失效旧令牌。 */
    public String generateToken(Long userId, String userName, String role, long ttlMs, long tokenVersion) {
        return generateToken(userId, userName, role, ttlMs, tokenVersion, false);
    }

    public String generateToken(Long userId, String userName, String role, long ttlMs,
                                long tokenVersion, boolean passwordUpgradeRequired) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userName", userName)
                .claim("role", role != null ? role : "user")
                .claim("tokenVersion", Math.max(0L, tokenVersion))
                .claim("passwordUpgradeRequired", passwordUpgradeRequired)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT（gateway 鉴权用）
     * @throws ExpiredJwtException 令牌已过期
     * @throws JwtException        签名非法/格式错误
     */
    public Claims parseToken(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 计算 JWT 剩余存活秒数（用于黑名单 TTL） */
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(0, diff / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (JwtException e) {
            return 0;
        }
    }

    /** 从 JWT 提取 userId */
    public Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(parseToken(token).getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取 AccessToken 有效期毫秒数 */
    public long getExpirationMs() {
        return expirationMs;
    }
}
