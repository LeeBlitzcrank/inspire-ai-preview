package com.inspire.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.PHONE_PATTERN;

@Data
public class SmsSendRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码用途不能为空")
    @Pattern(regexp = "login|bind", message = "验证码用途不正确")
    private String purpose;
}
