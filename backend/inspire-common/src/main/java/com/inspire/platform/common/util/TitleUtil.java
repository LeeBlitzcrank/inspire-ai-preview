/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/util/TitleUtil.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：通用工具类，提供可复用且无业务状态的静态能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.util;

/**
 * 灵感标题的统一长度约束，避免不同入口产生不一致数据。
 */
public final class TitleUtil {

    public static final int MAX_LENGTH = 16;

    private TitleUtil() {
    }

    public static int length(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return value.codePointCount(0, value.length());
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (length(normalized) <= MAX_LENGTH) {
            return normalized;
        }
        int end = normalized.offsetByCodePoints(0, MAX_LENGTH);
        return normalized.substring(0, end);
    }
}
