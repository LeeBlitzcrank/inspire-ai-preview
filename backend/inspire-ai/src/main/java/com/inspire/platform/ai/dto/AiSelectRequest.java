/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiSelectRequest.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AI_KEYWORD_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.CITY_MAX;

@Data
@Schema(description = "用户选中灵感请求")
public class AiSelectRequest {

    @Schema(description = "原始关键词", example = "鸡腿")
    @NotBlank(message = "关键词不能为空")
    @Size(max = AI_KEYWORD_MAX, message = "关键词不能超过100个字符")
    private String keyword;

    @Schema(description = "选中的灵感候选ID（1或2）", example = "1")
    @NotNull(message = "请选择一条灵感")
    @Min(value = 1, message = "灵感候选不正确")
    @Max(value = 20, message = "灵感候选不正确")
    private Integer selectedId;

    @Schema(description = "选中的灵感标题", example = "鸡腿的五种神仙吃法")
    @Size(max = 16, message = "灵感标题过长")
    private String selectedTitle;

    @Schema(description = "当前城市", example = "长沙")
    @Size(max = CITY_MAX, message = "城市名称过长")
    private String city;
}
