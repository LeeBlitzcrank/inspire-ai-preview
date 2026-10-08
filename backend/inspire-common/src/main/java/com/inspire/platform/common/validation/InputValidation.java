/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/validation/InputValidation.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：集中处理用户输入的归一化、长度校验、格式校验和 JSON 结构校验
 * 维护说明：Controller 校验只做第一道门，服务层必须调用这里的规则保证内部调用也安全。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static com.inspire.platform.common.validation.ValidationConstants.*;

/**
 * 统一输入校验工具。
 *
 * <p>规则的设计目标是避免两类问题：一是超长或异常字符把数据库拖入 500，
 * 二是前端限制被绕过之后产生脏数据。这里不做 HTML 转义，展示层仍应按上下文编码。</p>
 */
public final class InputValidation {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern USERNAME = Pattern.compile(USERNAME_PATTERN);
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Z0-9._%+\\-]+@[A-Z0-9.\\-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile(PHONE_PATTERN);
    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "12345678", "123456789", "password", "password1", "qwerty123",
            "11111111", "00000000", "abc12345", "admin123", "iloveyou",
            "letmein", "welcome1", "88888888", "66666666"
    );

    private InputValidation() {
    }

    /**
     * 注册和登录使用的账号归一化规则。
     */
    public static String normalizeUsername(String value) {
        String normalized = normalizeNfkc(value).trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(normalized).matches()) {
            throw badRequest("账号需为4-20位字母、数字或下划线");
        }
        return normalized;
    }

    /**
     * 登录时允许历史账号不满足新注册规则，但仍限制长度并做安全归一化。
     */
    public static String normalizeLoginUsername(String value) {
        String normalized = normalizeNfkc(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < USERNAME_MIN || codePointCount(normalized) > USERNAME_MAX) {
            throw badRequest("账号长度不正确");
        }
        return normalized;
    }

    public static String normalizeEmail(String value) {
        String normalized = normalizeNfkc(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw badRequest("邮箱不能为空");
        }
        if (codePointCount(normalized) > EMAIL_MAX || !EMAIL.matcher(normalized).matches()) {
            throw badRequest("邮箱格式或长度不正确");
        }
        return normalized;
    }

    public static String normalizePhone(String value) {
        String normalized = normalizeNfkc(value)
                .replaceAll("[\\s-]", "")
                .trim();
        if (!PHONE.matcher(normalized).matches()) {
            throw badRequest("请输入正确的11位手机号");
        }
        return normalized;
    }

    /**
     * 密码保存前做 Unicode 归一化，不 trim，避免用户密码中的空格被静默改变语义。
     */
    public static String normalizePassword(String value) {
        return normalizeNfkc(value);
    }

    public static void validatePassword(String password, String username, String email) {
        String normalized = normalizePassword(password);
        int length = codePointCount(normalized);
        if (length < PASSWORD_MIN || length > PASSWORD_MAX) {
            throw badRequest("密码长度需为8-64位");
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            throw badRequest("密码不能全部由数字组成");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (WEAK_PASSWORDS.contains(lower)) {
            throw badRequest("密码过于简单，请更换更安全的密码");
        }
        if (username != null && username.length() >= 3 && lower.contains(username.toLowerCase(Locale.ROOT))) {
            throw badRequest("密码不能包含账号");
        }
        if (email != null) {
            String localPart = email.substring(0, Math.max(0, email.indexOf('@'))).toLowerCase(Locale.ROOT);
            if (localPart.length() >= 3 && lower.contains(localPart)) {
                throw badRequest("密码不能包含邮箱前缀");
            }
        }
    }

    public static boolean isPasswordCompliant(String password, String username, String email) {
        try {
            validatePassword(password, username, email);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public static void validateLoginPassword(String password) {
        String normalized = normalizePassword(password);
        if (normalized.isEmpty() || codePointCount(normalized) > LOGIN_PASSWORD_MAX) {
            throw badRequest("密码长度不正确");
        }
    }

    /**
     * 昵称保留中文、字母、数字、常见符号和 emoji，但拒绝控制字符和格式控制符。
     */
    public static String normalizeNickname(String value) {
        String nfkc = normalizeNfkc(value);
        if (!hasOnlyDisplayableCharacters(nfkc)) {
            throw badRequest("昵称包含不可见或非法字符");
        }
        String normalized = collapseWhitespace(nfkc);
        int length = codePointCount(normalized);
        if (length < NICKNAME_MIN || length > NICKNAME_MAX) {
            throw badRequest("昵称长度需为2-20个字符");
        }
        return normalized;
    }

    public static String normalizeRequiredText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw badRequest(field + "不能为空");
        }
        if (codePointCount(normalized) > maxLength) {
            throw badRequest(field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    public static String normalizeOptionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (codePointCount(normalized) > maxLength) {
            throw badRequest(field + "不能超过" + maxLength + "个字符");
        }
        if (!hasOnlyDisplayableCharacters(normalized)) {
            throw badRequest(field + "包含非法字符");
        }
        return normalized;
    }

    public static String validateImageUrl(String value, String field) {
        String normalized = normalizeOptionalText(value, field, MEDIA_URL_MAX);
        if (normalized == null || normalized.isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (normalized.startsWith("/")) {
                return normalized;
            }
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw badRequest(field + "地址协议不受支持");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw badRequest(field + "地址不完整");
            }
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest(field + "地址格式不正确");
        }
    }

    /**
     * 图片列表必须是 JSON 数组，最多 9 张，每项为受支持的 URL。
     */
    public static String validateImagesJson(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        if (codePointCount(normalized) > IMAGE_JSON_MAX) {
            throw badRequest("图片列表数据过大");
        }
        try {
            JsonNode root = JSON.readTree(normalized);
            if (root == null || !root.isArray()) {
                throw badRequest("图片列表必须是数组");
            }
            if (root.size() > MAX_IMAGES) {
                throw badRequest("最多上传" + MAX_IMAGES + "张图片");
            }
            for (JsonNode item : root) {
                if (!item.isTextual()) {
                    throw badRequest("图片列表包含非法项");
                }
                validateImageUrl(item.asText(), "图片地址");
            }
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("图片列表格式不正确");
        }
    }

    public static String validateMessageExtraJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (codePointCount(normalized) > MESSAGE_EXTRA_JSON_MAX) {
            throw badRequest("消息扩展信息过大");
        }
        try {
            JsonNode root = JSON.readTree(normalized);
            if (root == null || !root.isObject()) {
                throw badRequest("消息扩展信息必须是对象");
            }
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("消息扩展信息格式不正确");
        }
    }

    public static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw badRequest(field + "不正确");
        }
    }

    public static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw badRequest(field + "不正确");
        }
    }

    public static void requirePage(int page, int size) {
        if (page < PAGE_MIN || size < PAGE_MIN || size > PAGE_SIZE_MAX) {
            throw badRequest("分页参数不正确");
        }
    }

    public static int codePointCount(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    private static String normalizeNfkc(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }

    private static String collapseWhitespace(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean previousWhitespace = false;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                if (!previousWhitespace) {
                    result.append(' ');
                    previousWhitespace = true;
                }
            } else {
                result.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
        }
        return result.toString().trim();
    }

    private static boolean hasOnlyDisplayableCharacters(String value) {
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isISOControl(codePoint)) {
                return false;
            }
            int type = Character.getType(codePoint);
            if (type == Character.FORMAT
                    || type == Character.SURROGATE
                    || type == Character.PRIVATE_USE
                    || type == Character.UNASSIGNED) {
                return false;
            }
        }
        return true;
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
}
