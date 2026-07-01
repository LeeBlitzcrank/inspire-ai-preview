package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspire_comment")
public class InspireComment {
    private Long id;
    private Long inspireId;
    private Long userId;
    private String username;
    private String avatar;
    private Long parentId;
    private Long replyUserId;
    private String replyUsername;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
