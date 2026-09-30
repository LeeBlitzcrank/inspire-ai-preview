/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/MessageConversation.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_conversation")
@Schema(description = "消息会话")
public class MessageConversation {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long user1Id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long user2Id;
    private String lastContent;
    private Long lastMessageId;
    private LocalDateTime lastTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String targetNickname;
    @TableField(exist = false)
    private Integer unreadUser1;
    @TableField(exist = false)
    private Integer unreadUser2;
}
