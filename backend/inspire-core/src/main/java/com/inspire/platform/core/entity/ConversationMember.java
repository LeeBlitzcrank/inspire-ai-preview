package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_member")
public class ConversationMember {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private Integer unreadCount;
    private LocalDateTime lastTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
