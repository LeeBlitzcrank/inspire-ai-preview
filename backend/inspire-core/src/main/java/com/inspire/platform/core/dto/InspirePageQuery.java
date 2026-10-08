/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/InspirePageQuery.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Data @Schema(description = "灵感分页查询参数")
public class InspirePageQuery {
    @Schema(description = "分类筛选", example = "美食")
    @Size(max = TAG_MAX, message = "分类名称过长")
    private String tag;

    @Schema(description = "页码从1开始", example = "1")
    @Min(value = PAGE_MIN, message = "页码不正确")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    @Min(value = PAGE_MIN, message = "每页条数不正确")
    @Max(value = PAGE_SIZE_MAX, message = "每页最多50条")
    private Integer size = 20;

    @Schema(description = "排序: time=最新 heat=最热", example = "time")
    @Pattern(regexp = "time|heat", message = "排序参数不正确")
    private String sort = "time";
}
