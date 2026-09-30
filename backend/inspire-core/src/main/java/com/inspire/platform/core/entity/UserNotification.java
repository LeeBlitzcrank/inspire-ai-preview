/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/UserNotification.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    private Long id;
    private Long userId;           // 通知接收者
    private String type;           // like / collect / comment / follow / mention / quote / special_publish
    private String targetType;     // inspire / comment / user
    private Long actorId;          // 触发者用户ID
    private String actorName;      // 触发者昵称
    private String content;        // 通知摘要文本
    private Long targetId;         // 关联灵感/评论ID
    private String targetTitle;    // 关联灵感标题
    private String eventKey;       // 幂等键
    private Integer isRead;        // 0未读 1已读
    private LocalDateTime readTime;
    private Integer deleted;
    private LocalDateTime createTime;
}
