package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 灵感分类（两级：parent_id = 0 为一级分类）
 * 由后台管理员维护，前台分类页读取。
 */
@Data
@TableName("sys_category")
public class Category {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父级ID，0 表示一级分类 */
    private Long parentId;

    private String name;

    /** 图标（emoji），二级分类可为空 */
    private String icon;

    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
