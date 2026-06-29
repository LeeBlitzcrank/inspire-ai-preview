package com.inspire.platform.core.dto;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Schema(description = "灵感展现层（列表/详情）")
public class InspireVO {
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "灵感ID") private Long id;
    @Schema(description = "标题") private String title;
    @Schema(description = "封面图") private String img;
    @Schema(description = "分类") private String tag;
    @Schema(description = "发布人用户名") private String username;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "点赞数") private Integer likeCount;
    @Schema(description = "收藏数") private Integer collectCount;
    @Schema(description = "热度") private Integer heat;
    @Schema(description = "发布城市") private String publishCity;
    @Schema(description = "发布时间") private LocalDateTime createTime;
    @Schema(description = "灵感正文（仅在详情接口返回）") private String content;
    @Schema(description = "当前用户是否已收藏") private Boolean collected = false;
    @Schema(description = "当前用户是否已点赞") private Boolean liked = false;
}
