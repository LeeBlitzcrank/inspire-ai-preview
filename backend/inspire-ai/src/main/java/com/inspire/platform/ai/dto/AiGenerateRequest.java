package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI灵感生成请求")
public class AiGenerateRequest {

    @Schema(description = "灵感关键词，AI将基于此生成创意", example = "鸡腿")
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    @Schema(description = "当前城市（用于同城热点计算）", example = "长沙")
    private String city;
}
