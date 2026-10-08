package com.inspire.platform.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AI_KEYWORD_MAX;

@Data
public class ImageSuggestRequest {
    @NotBlank(message = "配图关键词不能为空")
    @Size(max = AI_KEYWORD_MAX, message = "配图关键词不能超过100个字符")
    private String keyword;

    @Min(value = 1, message = "页码不正确")
    @Max(value = 100, message = "页码不能大于100")
    private Integer page = 1;
}
