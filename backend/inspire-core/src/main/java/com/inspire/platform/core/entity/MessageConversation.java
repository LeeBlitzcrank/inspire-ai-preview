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
    private LocalDateTime lastTime;
    private Integer unreadUser1;
    private Integer unreadUser2;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String targetNickname;
    @TableField(exist = false)
    private String targetUsername;
}
