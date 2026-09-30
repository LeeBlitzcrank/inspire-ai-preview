/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/CommentCreateRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    private Long inspireId;
    @NotBlank
    private String content;
    private String avatar;
    private Long parentId;
    private Long replyUserId;
    private String replyUsername;
}
