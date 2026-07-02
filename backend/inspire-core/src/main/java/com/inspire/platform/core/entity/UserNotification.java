package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    private Long id;
    private Long userId;           // 通知接收者
    private String type;           // like / collect / comment / follow
    private Long actorId;          // 触发者用户ID
    private String actorName;      // 触发者昵称
    private String content;        // 通知摘要文本
    private Long targetId;         // 关联灵感/评论ID
    private String targetTitle;    // 关联灵感标题
    private Integer isRead;        // 0未读 1已读
    private Integer deleted;
    private LocalDateTime createTime;
}
