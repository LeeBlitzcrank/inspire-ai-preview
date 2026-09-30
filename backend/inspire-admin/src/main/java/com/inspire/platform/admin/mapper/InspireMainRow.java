/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/mapper/InspireMainRow.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.mapper;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("inspire_main")
public class InspireMainRow {
    @TableId private Long id;
    private String title; private String img; private String tag; private Long userId;
    private Integer status; private Long viewCount; private Integer likeCount;
    private Integer collectCount; private Integer heat; private String publishCity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted; private String extJson;
}
