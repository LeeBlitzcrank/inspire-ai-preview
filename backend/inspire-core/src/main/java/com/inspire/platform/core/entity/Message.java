/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/Message.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
@Schema(description = "消息")
public class Message {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fromUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long toUserId;
    private String content;
    private String type;
    private String extraJson;
    private Integer isRead;
    private LocalDateTime recalledAt;
    @JsonIgnore
    private Long recalledBy;
    @JsonIgnore
    private String originalMessageHash;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
