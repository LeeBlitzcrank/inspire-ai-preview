/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/constant/RedisKeyConstant.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.constant;

/**
 * Redis 缓存 Key 常量（文档 4.6 节规范）
 * <p>
 * 统一管理所有缓存前缀，gateway / auth / common 共用同一套 Key 定义。
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {}

    // ========== 双Token 会话 ==========

    /** 刷新令牌 → 用户ID，TTL=7天 */
    public static final String REFRESH_PREFIX = "refresh:";

    /** 用户ID → 当前刷新令牌，TTL=7天，用于单点登录/踢人 */
    public static final String USER_REFRESH_PREFIX = "user_refresh:";

    // ========== 黑名单 ==========

    /** 失效 AccessToken 黑名单，TTL=JWT剩余秒 */
    public static final String BLACKLIST_PREFIX = "black_token:";

    // ========== 工具方法 ==========

    public static String refreshKey(String refreshToken) {
        return REFRESH_PREFIX + refreshToken;
    }

    public static String userRefreshKey(Long userId) {
        return USER_REFRESH_PREFIX + userId;
    }

    public static String blacklistKey(String accessToken) {
        return BLACKLIST_PREFIX + accessToken;
    }
}
