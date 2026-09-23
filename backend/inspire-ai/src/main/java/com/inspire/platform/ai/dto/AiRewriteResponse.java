package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "AI改写响应")
public class AiRewriteResponse {
    @Schema(description = "改写后的正文")
    private String text;
}
