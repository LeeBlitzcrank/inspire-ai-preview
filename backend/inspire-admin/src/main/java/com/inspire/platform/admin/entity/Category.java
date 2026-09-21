package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_category")
@Schema(description = "灵感分类（两级）")
public class Category {

    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "父级ID，0 表示一级分类")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标（emoji）")
    private String icon;

    @Schema(description = "排序，越小越靠前")
    private Integer sortOrder;

    @Schema(description = "1启用 0停用")
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
