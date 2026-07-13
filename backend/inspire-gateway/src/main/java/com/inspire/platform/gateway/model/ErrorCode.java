package com.inspire.platform.gateway.model;

/**
 * 鉴权错误码枚举
 * <p>
 * 严格对照文档 4.2.3 统一错误响应格式 & 第7章错误码统一定义表
 */
public enum ErrorCode {

    // ========== 401 认证失败 ==========

    /** 未携带登录令牌（请求头缺少 Authorization: Bearer） */
    TOKEN_MISSING(401001, "未携带登录令牌"),

    /** 令牌已失效（登出/后台踢人，Redis黑名单命中） */
    TOKEN_INVALIDATED(401002, "令牌已失效，请重新登录"),

    /** 令牌签名非法、篡改 */
    TOKEN_SIGNATURE_INVALID(401003, "令牌签名非法、篡改"),

    /** AccessToken已过期 */
    TOKEN_EXPIRED(401004, "登录令牌已过期，请刷新令牌"),

    /** RefreshToken不存在/过期 */
    REFRESH_TOKEN_INVALID(401005, "RefreshToken不存在或已过期"),

    // ========== 403 权限不足 ==========

    /** 当前角色无接口访问权限 */
    FORBIDDEN(403001, "当前角色无接口访问权限");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
