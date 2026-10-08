package com.inspire.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.PHONE_PATTERN;

@Data
public class SmsCodeVerifyRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    private String code;

    @Size(max = 128, message = "设备标识过长")
    private String deviceId;
}
