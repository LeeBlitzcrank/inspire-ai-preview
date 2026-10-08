/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiExploreRequest.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Data @Schema(description = "AI探索请求")
public class AiExploreRequest {
    @Schema(description = "关键词", example = "鸡腿")
    @NotBlank(message = "关键词不能为空")
    @Size(max = AI_KEYWORD_MAX, message = "关键词不能超过100个字符")
    private String keyword;

    @Schema(description = "当前路径（逗号分隔）", example = "opt1,detail2")
    @Size(max = AI_PATH_MAX, message = "探索路径过长")
    private String path;

    @Schema(description = "是否刷新（跳过缓存重新生成）", example = "false")
    private boolean refresh;

    @Schema(description = "期望生成的风格数量（1-5，默认 3）", example = "3")
    @Min(value = 1, message = "风格数量需为1-5")
    @Max(value = AI_VARIANTS_MAX, message = "风格数量需为1-5")
    private Integer variants;
}
