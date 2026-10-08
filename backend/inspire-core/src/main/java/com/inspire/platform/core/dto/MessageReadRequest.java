package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MessageReadRequest {
    @NotNull(message = "缺少会话ID")
    @Positive(message = "会话ID不正确")
    private Long conversationId;
}
