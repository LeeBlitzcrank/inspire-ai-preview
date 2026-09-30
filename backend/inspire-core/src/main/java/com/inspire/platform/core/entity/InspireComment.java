/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/InspireComment.java
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
