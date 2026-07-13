package com.inspire.platform.common.model;

/**
 * 当前登录用户上下文（ThreadLocal）
 * <p>
 * 由 {@link com.inspire.platform.common.interceptor.UserContextInterceptor}
 * 在每个 HTTP 请求开始时从网关透传的请求头中解析并注入。
 * <p>
 * 下游业务微服务通过静态方法获取当前用户信息，无需解析 JWT。
 * 文档 6.3 节：下游服务不引入JWT依赖，仅读取网关透传 Header。
 * <p>
 * 使用示例：
 * <pre>{@code
 * Long userId = UserContext.getUserId();
 * String role = UserContext.getRole();
 * if (UserContext.isLogin()) { ... }
 * }</pre>
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    // ========== Setter（仅由 Interceptor 调用） ==========

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static void setRole(String role) {
        ROLE_HOLDER.set(role);
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    // ========== Getter（业务代码使用） ==========

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    // ========== 快捷方法 ==========

    /**
     * 当前请求是否已登录（携带有效令牌并由网关解析通过）
     */
    public static boolean isLogin() {
        return USER_ID_HOLDER.get() != null;
    }

    /**
     * 获取当前用户ID（如果未登录则抛出 BusinessException）
     *
     * @throws IllegalStateException 未登录
     */
    public static Long requireUserId() {
        Long id = USER_ID_HOLDER.get();
        if (id == null) {
            throw new IllegalStateException("当前请求未登录，无法获取用户ID");
        }
        return id;
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param role 角色标识（admin / core / user）
     * @return true=拥有该角色
     */
    public static boolean hasRole(String role) {
        String currentRole = ROLE_HOLDER.get();
        return currentRole != null && currentRole.equalsIgnoreCase(role);
    }

    // ========== 清理（Interceptor 在请求完成后调用） ==========

    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
