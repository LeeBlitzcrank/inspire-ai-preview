/**
 * 文件：backend/inspire-common/src/test/java/com/inspire/platform/common/interceptor/UserContextInterceptorTest.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：自动化测试类，验证对应模块的边界行为和回归场景
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.interceptor;

import com.inspire.platform.common.util.InternalAuthUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContextInterceptorTest {

    private static final String SECRET = "test-internal-secret-32-bytes-long!!";

    @Test
    void rejectsForgedHeadersAndAcceptsSignedHeaders() throws Exception {
        UserContextInterceptor interceptor = new UserContextInterceptor(SECRET);
        MockHttpServletRequest forged = new MockHttpServletRequest("GET", "/inspire/my");
        forged.addHeader(InternalAuthUtil.USER_ID_HEADER, "100");
        forged.addHeader(InternalAuthUtil.USER_ROLE_HEADER, "admin");
        assertFalse(interceptor.preHandle(forged, new MockHttpServletResponse(), new Object()));

        MockHttpServletRequest trusted = new MockHttpServletRequest("GET", "/inspire/my");
        long timestamp = System.currentTimeMillis();
        trusted.addHeader(InternalAuthUtil.USER_ID_HEADER, "100");
        trusted.addHeader(InternalAuthUtil.USER_ROLE_HEADER, "user");
        trusted.addHeader(InternalAuthUtil.TIMESTAMP_HEADER, String.valueOf(timestamp));
        trusted.addHeader(InternalAuthUtil.SIGNATURE_HEADER, InternalAuthUtil.sign(
                SECRET, "100", "user", timestamp, "GET", "/inspire/my", null));
        assertTrue(interceptor.preHandle(trusted, new MockHttpServletResponse(), new Object()));
    }
}
