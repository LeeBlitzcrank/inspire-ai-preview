package com.inspire.platform.core.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AI_KEYWORD_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.AI_PATH_MAX;

@Data
public class AiHistorySaveRequest {
    @NotBlank(message = "探索关键词不能为空")
    @Size(max = AI_KEYWORD_MAX, message = "探索关键词不能超过100个字符")
    private String keyword;

    @Size(max = AI_PATH_MAX, message = "探索路径过长")
    private String path;

    @Size(max = 600, message = "缓存标识过长")
    private String cacheKey;

    @NotNull(message = "探索结果不能为空")
    private JsonNode result;
}
