/**
 * 文件：backend/inspire-common/src/test/java/com/inspire/platform/common/util/InternalAuthUtilTest.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：自动化测试类，验证对应模块的边界行为和回归场景
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalAuthUtilTest {

    private static final String SECRET = "test-internal-secret-32-bytes-long!!";

    @Test
    void acceptsValidSignature() {
        long timestamp = System.currentTimeMillis();
        String signature = InternalAuthUtil.sign(
                SECRET, "100", "user", timestamp, "GET", "/inspire/my", "page=1");
        assertTrue(InternalAuthUtil.verify(
                SECRET, "100", "user", String.valueOf(timestamp), signature,
                "GET", "/inspire/my", "page=1"));
    }

    @Test
    void rejectsTamperedSignatureAndExpiredTimestamp() {
        long timestamp = System.currentTimeMillis();
        String signature = InternalAuthUtil.sign(
                SECRET, "100", "user", timestamp, "GET", "/inspire/my", null);
        assertFalse(InternalAuthUtil.verify(
                SECRET, "100", "admin", String.valueOf(timestamp), signature,
                "GET", "/inspire/my", null));
        assertFalse(InternalAuthUtil.verify(
                SECRET, "100", "user", String.valueOf(timestamp - 300_000), signature,
                "GET", "/inspire/my", null));
    }
}
