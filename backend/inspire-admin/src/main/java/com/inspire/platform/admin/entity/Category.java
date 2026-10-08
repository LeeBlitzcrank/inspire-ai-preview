/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/entity/Category.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
    @PositiveOrZero(message = "父级分类不正确")
    private Long parentId;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    @Schema(description = "图标（emoji）")
    @Size(max = 20, message = "分类图标过长")
    private String icon;

    @Schema(description = "排序，越小越靠前")
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sortOrder;

    @Schema(description = "1启用 0停用")
    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
