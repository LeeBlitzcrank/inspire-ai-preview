/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiExploreResponse.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data @Schema(description = "AI探索响应")
public class AiExploreResponse {
    @Schema(description = "当前层摘要") private String summary;
    @Schema(description = "子选项（非叶子节点时不为空）") private List<Option> options;
    @Schema(description = "最终内容（叶子节点时不为空）") private LeafContent content;
    @Schema(description = "缓存key", example = "鸡腿") private String cacheKey;
    @Schema(description = "是否直接命中缓存（未消耗 token）") private boolean fromCache;

    @Data
    public static class Option {
        @Schema(description = "选项ID", example = "cook") private String id;
        @Schema(description = "选项标签", example = "做法") private String label;
    }

    @Data
    public static class LeafContent {
        @Schema(description = "灵感标题") private String title;
        @Schema(description = "灵感正文") private String text;
        @Schema(description = "分类", example = "美食") private String tag;
        @Schema(description = "多风格候选文案，前端可切换") private List<Variant> variants;
    }

    @Data
    @Schema(description = "一种风格的候选文案")
    public static class Variant {
        @Schema(description = "风格名", example = "温柔治愈") private String style;
        @Schema(description = "灵感标题") private String title;
        @Schema(description = "灵感正文") private String text;
    }
}
