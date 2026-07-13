package com.inspire.platform.common.util;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.model.ErrorCode;
import com.inspire.platform.common.model.UserContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限校验工具类
 * <p>
 * 文档 6.3 节：基于角色实现接口权限控制。
 * 下游业务服务使用此工具类判断当前登录用户是否有权执行操作。
 * <p>
 * 拒绝访问时抛出 {@link BusinessException}，错误码 403001。
 *
 * <pre>{@code
 * // 仅管理员可访问
 * PermissionUtil.requireRole("admin");
 *
 * // 管理员 或 灵感创作者 可访问
 * PermissionUtil.requireRole("admin", "core");
 *
 * // 条件判断
 * if (PermissionUtil.hasRole("admin")) { ... }
 * }</pre>
 */
public final class PermissionUtil {

    private PermissionUtil() {
        // 工具类禁止实例化
    }

    /**
     * 要求当前用户拥有指定角色之一
     *
     * @param roles 允许的角色列表（如 "admin", "core"）
     * @throws BusinessException 403001 — 无权限
     */
    public static void requireRole(String... roles) {
        if (!hasRole(roles)) {
            String userRole = UserContext.getRole();
            throw new BusinessException(
                    ErrorCode.FORBIDDEN.getCode(),
                    "当前角色 [" + (userRole != null ? userRole : "未登录") + "] 无权访问，需要角色: "
                            + String.join(", ", roles)
            );
        }
    }

    /**
     * 判断当前用户是否拥有指定角色之一
     *
     * @param roles 允许的角色列表
     * @return true=拥有权限
     */
    public static boolean hasRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        String currentRole = UserContext.getRole();
        if (currentRole == null) {
            return false;
        }
        Set<String> roleSet = Arrays.stream(roles)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return roleSet.contains(currentRole.toLowerCase());
    }

    /**
     * 要求当前用户已登录
     *
     * @throws BusinessException 401001 — 未登录
     */
    public static void requireLogin() {
        if (!UserContext.isLogin()) {
            throw new BusinessException(
                    ErrorCode.TOKEN_MISSING.getCode(),
                    ErrorCode.TOKEN_MISSING.getMessage()
            );
        }
    }

    /**
     * 校验当前用户是否为资源所有者（防止越权操作）
     *
     * @param resourceUserId 资源所属的用户ID
     * @throws BusinessException 403001 — 非资源所有者
     */
    public static void requireOwner(Long resourceUserId) {
        if (!UserContext.isLogin()) {
            throw new BusinessException(
                    ErrorCode.TOKEN_MISSING.getCode(),
                    ErrorCode.TOKEN_MISSING.getMessage()
            );
        }
        Long currentUserId = UserContext.getUserId();
        if (!currentUserId.equals(resourceUserId)) {
            // 管理员可以覆盖
            String role = UserContext.getRole();
            if ("admin".equalsIgnoreCase(role)) {
                return;
            }
            throw new BusinessException(
                    ErrorCode.FORBIDDEN.getCode(),
                    "无权操作其他用户的资源"
            );
        }
    }
}
