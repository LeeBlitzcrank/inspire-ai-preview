/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiTitleRequest.java
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

@Data
@Schema(description = "AI生成标题请求")
public class AiTitleRequest {
    @NotBlank
    @Size(max = 2000)
    @Schema(description = "灵感正文")
    private String content;
}
