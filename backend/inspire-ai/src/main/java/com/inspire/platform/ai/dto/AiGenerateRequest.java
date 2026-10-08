/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiGenerateRequest.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AI_KEYWORD_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.CITY_MAX;

@Data
@Schema(description = "AI灵感生成请求")
public class AiGenerateRequest {

    @Schema(description = "灵感关键词，AI将基于此生成创意", example = "鸡腿")
    @NotBlank(message = "关键词不能为空")
    @Size(max = AI_KEYWORD_MAX, message = "关键词不能超过100个字符")
    private String keyword;

    @Schema(description = "当前城市（用于同城热点计算）", example = "长沙")
    @Size(max = CITY_MAX, message = "城市名称过长")
    private String city;
}
