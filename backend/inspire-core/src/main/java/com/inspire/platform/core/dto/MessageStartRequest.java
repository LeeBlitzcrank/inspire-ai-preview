package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MessageStartRequest {
    @NotNull(message = "缺少接收人")
    @Positive(message = "接收人不正确")
    private Long toUserId;
}
