package com.inspire.platform.core.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AI_KEYWORD_MAX;

@Data
public class AiCallLogRequest {
    @Size(max = AI_KEYWORD_MAX, message = "AI调用关键词过长")
    private String keyword;

    @PositiveOrZero(message = "用户ID不正确")
    private Long userId;
}
