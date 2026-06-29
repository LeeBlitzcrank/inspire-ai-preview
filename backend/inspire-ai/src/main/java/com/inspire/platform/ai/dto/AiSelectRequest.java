package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户选中灵感请求")
public class AiSelectRequest {

    @Schema(description = "原始关键词", example = "鸡腿")
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    @Schema(description = "选中的灵感候选ID（1或2）", example = "1")
    @NotNull(message = "请选择一条灵感")
    private Integer selectedId;

    @Schema(description = "选中的灵感标题", example = "鸡腿的五种神仙吃法")
    private String selectedTitle;

    @Schema(description = "当前城市", example = "长沙")
    private String city;
}
