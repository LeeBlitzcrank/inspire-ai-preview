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
    }
}
