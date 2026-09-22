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
