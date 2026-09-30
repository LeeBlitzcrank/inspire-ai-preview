/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/InspireContent.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("inspire_content")
@Schema(description = "灵感正文（冷热分离冷数据）")
public class InspireContent {
    @Schema(description = "关联灵感ID") @TableId private Long inspireId;
    @Schema(description = "灵感完整正文") private String content;
}
