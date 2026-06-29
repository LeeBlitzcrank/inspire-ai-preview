package com.inspire.platform.search.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Schema(description = "搜索结果项")
public class SearchResultVO {
    @Schema(description = "灵感ID", example = "197197582563282945") private Long id;
    @Schema(description = "灵感标题", example = "鸡腿的五种神仙吃法") private String title;
    @Schema(description = "封面图") private String img;
    @Schema(description = "分类", example = "美食") private String tag;
    @Schema(description = "热度") private Integer heat;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "点赞数") private Integer likeCount;
    @Schema(description = "收藏数") private Integer collectCount;
    @Schema(description = "发布城市", example = "长沙") private String publishCity;
    @Schema(description = "发布时间") private LocalDateTime createTime;
    @Schema(description = "搜索来源", example = "es / mysql") private String source;
}
