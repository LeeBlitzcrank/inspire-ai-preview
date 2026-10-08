/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/CommentCreateRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.AVATAR_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.COMMENT_MAX;

@Data
public class CommentCreateRequest {
    @Positive(message = "灵感ID不正确")
    private Long inspireId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = COMMENT_MAX, message = "评论不能超过500个字符")
    private String content;

    @Size(max = AVATAR_MAX, message = "头像地址过长")
    private String avatar;

    @Positive(message = "回复目标不正确")
    private Long parentId;

    @Positive(message = "回复用户不正确")
    private Long replyUserId;

    @Size(max = 60, message = "回复昵称过长")
    private String replyUsername;
}
