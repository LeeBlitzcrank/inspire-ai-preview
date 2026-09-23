package com.inspire.platform.core.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Schema(description = "灵感展现层（列表/详情）")
public class InspireVO {
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "灵感ID") private Long id;
    @Schema(description = "标题") private String title;
    @Schema(description = "封面图") private String img;
    @Schema(description = "多图列表") private java.util.List<String> images;
    @Schema(description = "系列ID") private String seriesId;
    @Schema(description = "系列名称") private String seriesName;
    @Schema(description = "系列内顺序") private Integer seriesOrder;
    @Schema(description = "系列文章总数") private Integer seriesTotal;
    @Schema(description = "上一篇ID") private String prevSeriesId;
    @Schema(description = "上一篇标题") private String prevSeriesTitle;
    @Schema(description = "下一篇ID") private String nextSeriesId;
    @Schema(description = "下一篇标题") private String nextSeriesTitle;
    @Schema(description = "分类") private String tag;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "一级分类ID") private Long categoryId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "二级分类ID") private Long subCategoryId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "发布人ID") private Long userId;
    @Schema(description = "发布人用户名") private String username;
    @Schema(description = "发布人昵称") private String nickname;
    @Schema(description = "头像") private String avatar;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "点赞数") private Integer likeCount;
    @Schema(description = "收藏数") private Integer collectCount;
    @Schema(description = "热度") private Integer heat;
    @Schema(description = "分享数") private Integer shareCount;    @Schema(description = "发布城市") private String publishCity;
    @Schema(description = "发布时间") private LocalDateTime createTime;
    @Schema(description = "灵感正文（仅在详情接口返回）") private String content;
    @Schema(description = "当前用户是否已收藏") private Boolean collected = false;
    @Schema(description = "当前用户是否已点赞") private Boolean liked = false;
    @Schema(description = "当前登录用户是否已关注作者") private Boolean following = false;
}
