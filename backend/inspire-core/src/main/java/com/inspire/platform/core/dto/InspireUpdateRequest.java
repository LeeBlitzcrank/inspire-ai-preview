package com.inspire.platform.core.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data @Schema(description = "更新灵感请求（不传的字段不修改）")
public class InspireUpdateRequest {
    @Schema(description = "标题") private String title;
    @Schema(description = "正文") private String content;
    @Schema(description = "分类") private String tag;
    @Schema(description = "封面图") private String img;
    @Schema(description = "多图JSON数组") private String images;
    @Schema(description = "0草稿 1发布") private Integer status;
    @Schema(description = "发布城市") private String publishCity;
}
