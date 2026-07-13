package com.inspire.platform.common.model;

/**
 * 鉴权错误码枚举（共享版 · 下游业务微服务使用）
 * <p>
 * 严格对照文档 4.2.3 / 第7章错误码统一定义表。
 * 网关层的同名枚举（gateway.model.ErrorCode）与其保持同步。
 */
public enum ErrorCode {

    // ========== 401 认证失败 ==========
    TOKEN_MISSING(401001, "未携带登录令牌"),
    TOKEN_INVALIDATED(401002, "令牌已失效，请重新登录"),
    TOKEN_SIGNATURE_INVALID(401003, "令牌签名非法、篡改"),
    TOKEN_EXPIRED(401004, "登录令牌已过期，请刷新令牌"),
    REFRESH_TOKEN_INVALID(401005, "RefreshToken不存在或已过期"),

    // ========== 403 权限不足 ==========
    FORBIDDEN(403001, "当前角色无接口访问权限"),
    FORBIDDEN_ROLE(403002, "当前角色无权执行此操作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
