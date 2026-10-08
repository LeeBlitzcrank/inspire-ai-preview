package com.inspire.platform.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiHistorySelectRequest {
    @Min(value = 0, message = "候选序号不正确")
    @Max(value = 20, message = "候选序号不正确")
    private Integer selectedIndex;

    @Size(max = 16, message = "标题不能超过16个字符")
    private String selectedTitle;
}
