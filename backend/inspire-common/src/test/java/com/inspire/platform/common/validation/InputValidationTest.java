/**
 * 文件：backend/inspire-common/src/test/java/com/inspire/platform/common/validation/InputValidationTest.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：自动化测试类，验证输入边界和归一化规则
 * 维护说明：这些规则被多个业务模块复用，测试失败时不要只改测试，要确认线上入口是否受影响。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.validation;

import com.inspire.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidationTest {

    @Test
    void normalizesUsernameAndEmail() {
        assertEquals("user_001", InputValidation.normalizeUsername(" User_001 "));
        assertEquals("user001@example.com", InputValidation.normalizeEmail(" USER001@EXAMPLE.COM "));
    }

    @Test
    void rejectsInvalidUsernameAndEmail() {
        assertThrows(BusinessException.class, () -> InputValidation.normalizeUsername("用户001"));
        assertThrows(BusinessException.class, () -> InputValidation.normalizeEmail("not-an-email"));
    }

    @Test
    void normalizesAndValidatesPhone() {
        assertEquals("13800138000", InputValidation.normalizePhone("138 0013 8000"));
        assertThrows(BusinessException.class, () -> InputValidation.normalizePhone("12345"));
    }

    @Test
    void enforcesPasswordPolicy() {
        InputValidation.validatePassword("StrongPass123", "user001", "user001@example.com");
        assertThrows(BusinessException.class, () -> InputValidation.validatePassword("12345678", "user001", "user001@example.com"));
        assertThrows(BusinessException.class, () -> InputValidation.validatePassword("User001Pass", "user001", "user001@example.com"));
    }

    @Test
    void normalizesNicknameAndRejectsInvisibleCharacters() {
        assertEquals("清风 明月", InputValidation.normalizeNickname(" 清风   明月 "));
        assertThrows(BusinessException.class, () -> InputValidation.normalizeNickname("清\n风"));
        assertThrows(BusinessException.class, () -> InputValidation.normalizeNickname("一"));
    }

    @Test
    void validatesImageJsonAndMessageExtraJson() {
        assertEquals("[\"/uploads/a.webp\"]",
                InputValidation.validateImagesJson("[\"/uploads/a.webp\"]"));
        assertThrows(BusinessException.class, () -> InputValidation.validateImagesJson("{\"url\":\"/a.webp\"}"));
        assertThrows(BusinessException.class, () -> InputValidation.validateImagesJson(
                "[\"/1\",\"/2\",\"/3\",\"/4\",\"/5\",\"/6\",\"/7\",\"/8\",\"/9\",\"/10\"]"));
        assertEquals("{\"id\":\"1\"}", InputValidation.validateMessageExtraJson("{\"id\":\"1\"}"));
        assertThrows(BusinessException.class, () -> InputValidation.validateMessageExtraJson("[]"));
    }
}
