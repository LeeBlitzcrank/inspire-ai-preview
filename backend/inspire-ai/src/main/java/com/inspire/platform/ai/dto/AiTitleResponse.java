package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "AI标题候选响应")
public class AiTitleResponse {
    @Schema(description = "5个标题候选")
    private List<String> titles;
}
