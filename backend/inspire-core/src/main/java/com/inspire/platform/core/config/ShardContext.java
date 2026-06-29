package com.inspire.platform.core.config;

/**
 * 分表上下文：通过 ThreadLocal 传递分表后缀
 * 在调用 Mapper 前设置，MyBatis-Plus DynamicTableNameInnerInterceptor 自动拼接表名
 *
 * 收藏分表：user_id % 10 → collect_{n}
 * 点赞分表：inspire_id % 10 → inspire_like_{n}
 */
public class ShardContext {
    private static final ThreadLocal<String> SUFFIX = new ThreadLocal<>();

    public static void set(String suffix) { SUFFIX.set(suffix); }
    public static String get() { return SUFFIX.get(); }
    public static void clear() { SUFFIX.remove(); }

    public static void setByUserId(Long userId) { set(String.valueOf(userId % 10)); }
    public static void setByInspireId(Long inspireId) { set(String.valueOf(inspireId % 10)); }
}
