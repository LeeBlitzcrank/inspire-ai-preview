package com.inspire.platform.gateway.util;

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
 * JWT 令牌工具类（双Token体系 — AccessToken）
 * <p>
 * 文档 4.1.2 节：HS256 加密，载荷仅存放 userId / userName / role 非敏感数据。
 * 密钥从环境变量 INSPIRE_JWT_SECRET 注入，开发/生产密钥隔离。
 * <p>
 * parseToken 方法不吞异常，由调用方（JwtAuthGlobalFilter）区分失效原因并返回对应错误码：
 * — ExpiredJwtException → 401004（令牌过期）
 * — 其他 JwtException   → 401003（签名篡改/非法）
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * @param secret     Base64 编码的 HMAC-SHA 密钥（≥256位/32字节）
     * @param expiration AccessToken 过期毫秒数，文档规定 900秒（15分钟）
     */
    public JwtUtil(
            @Value("${inspire.jwt.secret}") String secret,
            @Value("${inspire.jwt.access-token-expiration:900000}") long expiration) {
        // 优先 Base64 解码（生产推荐），失败后降级为 UTF-8 字节（开发环境备用）
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            log.warn("JWT密钥非Base64格式，使用UTF-8字节降级（仅建议开发环境使用）");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expiration;
        log.info("JwtUtil初始化完成，密钥长度={}位，AccessToken有效期={}ms",
                keyBytes.length * 8, expiration);
    }

    /**
     * 生成 AccessToken（JWT）
     * <p>
     * 文档 4.1.2 载荷规范：
     * — sub:      userId
     * — userName: 用户名
     * — role:     角色标识（admin / core / user）
     * — iat:      签发时间
     * — exp:      过期时间
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
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT 令牌
     * <p>
     * <b>注意：</b>该方法不吞异常，调用方负责区分异常类型并返回正确错误码。
     * — ExpiredJwtException：令牌已过期，签名本身合法
     * — 其他 JwtException：  签名篡改、格式非法等
     *
     * @param token JWT 字符串
     * @return 解析成功的 Claims
     * @throws ExpiredJwtException 令牌已过期
     * @throws JwtException        签名非法、格式错误等其他异常
     */
    public Claims parseToken(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 AccessToken 的有效期（毫秒）
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
