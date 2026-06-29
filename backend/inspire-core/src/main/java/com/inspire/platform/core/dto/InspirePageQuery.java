package com.inspire.platform.core.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data @Schema(description = "灵感分页查询参数")
public class InspirePageQuery {
    @Schema(description = "分类筛选", example = "美食") private String tag;
    @Schema(description = "页码从1开始", example = "1") private Integer page = 1;
    @Schema(description = "每页条数", example = "20") private Integer size = 20;
    @Schema(description = "排序: time=最新 heat=最热", example = "time") private String sort = "time";
}
