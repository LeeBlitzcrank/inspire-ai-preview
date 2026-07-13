package com.inspire.platform.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回结果封装
 * 对应 PRD 2.4 统一返回格式规范
 * 文档第7章标准错误码常量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    // ========== HTTP 状态码（快捷映射） ==========
    public static final int HTTP_OK = 200;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_INTERNAL_ERROR = 500;

    // ========== 文档第7章 401 认证失败系列 ==========
    public static final int TOKEN_MISSING = 401001;
    public static final int TOKEN_INVALIDATED = 401002;
    public static final int TOKEN_SIGNATURE_INVALID = 401003;
    public static final int TOKEN_EXPIRED = 401004;
    public static final int REFRESH_TOKEN_INVALID = 401005;

    // ========== 文档第7章 403 权限不足系列 ==========
    public static final int FORBIDDEN_NO_PERMISSION = 403001;

    private int code;
    private String msg;
    private T data;

    // ========== 成功 ==========

    public static <T> Result<T> success() {
        return new Result<>(HTTP_OK, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(HTTP_OK, "操作成功", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(HTTP_OK, msg, data);
    }

    // ========== 失败（通用） ==========

    /** 按文档标准错误码返回失败结果 */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(HTTP_INTERNAL_ERROR, msg, null);
    }

    // ========== 文档标准快捷方法 ==========

    /** 401001 未携带登录令牌 */
    public static <T> Result<T> tokenMissing() {
        return new Result<>(TOKEN_MISSING, "未携带登录令牌", null);
    }

    /** 401002 令牌已失效（登出/后台踢人） */
    public static <T> Result<T> tokenInvalidated() {
        return new Result<>(TOKEN_INVALIDATED, "令牌已失效，请重新登录", null);
    }

    /** 401003 令牌签名非法、篡改 */
    public static <T> Result<T> tokenSignatureInvalid() {
        return new Result<>(TOKEN_SIGNATURE_INVALID, "令牌签名非法、篡改", null);
    }

    /** 401004 AccessToken已过期 */
    public static <T> Result<T> tokenExpired() {
        return new Result<>(TOKEN_EXPIRED, "登录令牌已过期，请刷新令牌", null);
    }

    /** 401005 RefreshToken不存在/过期 */
    public static <T> Result<T> refreshTokenInvalid() {
        return new Result<>(REFRESH_TOKEN_INVALID, "RefreshToken不存在或已过期", null);
    }

    /** 403001 当前角色无接口访问权限 */
    public static <T> Result<T> forbidden() {
        return new Result<>(FORBIDDEN_NO_PERMISSION, "当前角色无接口访问权限", null);
    }

    /**
     * 根据文档错误码快速构建
     *
     * @param code 文档错误码
     * @return Result 实例
     */
    public static <T> Result<T> of(int code) {
        return switch (code) {
            case TOKEN_MISSING -> tokenMissing();
            case TOKEN_INVALIDATED -> tokenInvalidated();
            case TOKEN_SIGNATURE_INVALID -> tokenSignatureInvalid();
            case TOKEN_EXPIRED -> tokenExpired();
            case REFRESH_TOKEN_INVALID -> refreshTokenInvalid();
            case FORBIDDEN_NO_PERMISSION -> forbidden();
            default -> new Result<>(code, "操作失败", null);
        };
    }
}
