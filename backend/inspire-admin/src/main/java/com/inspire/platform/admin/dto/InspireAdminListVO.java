/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/dto/InspireAdminListVO.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
@Data @Schema(description = "管理后台 - 灵感列表项")
public class InspireAdminListVO {
    @Schema(description = "灵感ID", example = "197197582563282945")
    @JsonSerialize(using = ToStringSerializer.class) private Long id;
    @Schema(description = "标题") private String title;
    @Schema(description = "分类") private String tag;
    @Schema(description = "发布人用户名") private String username;
    @Schema(description = "0草稿 1已发布") private Integer status;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "点赞数") private Integer likeCount;
    @Schema(description = "收藏数") private Integer collectCount;
    @Schema(description = "热度") private Integer heat;
    @Schema(description = "发布城市") private String publishCity;
    @Schema(description = "0正常 1已下架") private Integer deleted;
    @Schema(description = "发布时间") private LocalDateTime createTime;
}
