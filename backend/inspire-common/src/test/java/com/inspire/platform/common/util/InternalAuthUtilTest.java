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
