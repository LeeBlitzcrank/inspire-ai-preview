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
