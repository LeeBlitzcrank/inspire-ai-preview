package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI生成标题请求")
public class AiTitleRequest {
    @NotBlank
    @Size(max = 2000)
    @Schema(description = "灵感正文")
    private String content;
}
