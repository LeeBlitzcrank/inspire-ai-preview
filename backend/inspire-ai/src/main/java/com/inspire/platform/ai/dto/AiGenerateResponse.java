package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "AI灵感生成响应")
public class AiGenerateResponse {

    @Schema(description = "输入的关键词", example = "鸡腿")
    private String keyword;

    @Schema(description = "AI返回的2条灵感候选")
    private List<InspirationCandidate> candidates;
}
