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
    private String authorNickname;
    private String avatar;
    private Long parentId;
    private Long rootId;
    private Long replyUserId;
    private String replyNickname;
    private String content;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
