package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI改写请求")
public class AiRewriteRequest {
    @NotBlank
    @Size(max = 1200)
    @Schema(description = "选中正文")
    private String text;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "目标风格", example = "简洁")
    private String style;
}
