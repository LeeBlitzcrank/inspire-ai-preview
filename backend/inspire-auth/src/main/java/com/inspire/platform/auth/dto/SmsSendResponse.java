package com.inspire.platform.auth.dto;

public record SmsSendResponse(
        int cooldownSeconds,
        String devCode
) {
}
