package com.inspire.platform.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 网关到业务服务之间的内部身份签名。
 */
public final class InternalAuthUtil {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Internal-Signature";
    public static final long DEFAULT_MAX_SKEW_MS = 120_000L;

    private InternalAuthUtil() {
    }

    public static String sign(String secret, String userId, String role, long timestamp,
                              String method, String path, String query) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(canonical(userId, role, timestamp, method, path, query)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("无法生成内部身份签名", e);
        }
    }

    public static boolean verify(String secret, String userId, String role, String timestamp,
                                 String signature, String method, String path, String query) {
        if (secret == null || secret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - ts) > DEFAULT_MAX_SKEW_MS) {
                return false;
            }
            String expected = sign(secret, userId, role, ts, method, path, query);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.toLowerCase().getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    private static String canonical(String userId, String role, long timestamp,
                                    String method, String path, String query) {
        return String.join("\n",
                nullToEmpty(userId),
                nullToEmpty(role),
                String.valueOf(timestamp),
                nullToEmpty(method).toUpperCase(),
                nullToEmpty(path),
                nullToEmpty(query));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
