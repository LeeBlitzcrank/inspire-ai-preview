/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/LikeAction.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_like")
@Schema(description = "用户点赞记录（user_like 按 user_id HASH 分区）")
public class LikeAction {
    private Long id;
    @Schema(description = "点赞用户ID") private Long userId;
    private Long inspireId;
    private LocalDateTime createTime;
}
