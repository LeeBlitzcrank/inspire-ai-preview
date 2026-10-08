/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/entity/RecommendPush.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recommend_push")
public class RecommendPush {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspireId;
    private String targetType;
    private String targetValue;
    private BigDecimal weight;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private String img;
}
