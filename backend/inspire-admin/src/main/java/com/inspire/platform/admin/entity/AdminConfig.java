/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/entity/AdminConfig.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：Spring 配置类，负责基础设施或框架能力装配
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
@Data @TableName("admin_config") @Schema(description = "运营配置")
public class AdminConfig {
    @Schema(description = "配置ID") @TableId(type = IdType.AUTO) private Integer id;
    @Schema(description = "配置key", example = "recommend.hot_weight") private String configKey;
    @Schema(description = "配置值", example = "0.6") private String configValue;
    @Schema(description = "配置说明") @TableField("`desc`")
    private String desc;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
