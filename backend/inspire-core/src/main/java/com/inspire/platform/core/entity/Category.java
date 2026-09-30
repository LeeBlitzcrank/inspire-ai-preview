/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/Category.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
