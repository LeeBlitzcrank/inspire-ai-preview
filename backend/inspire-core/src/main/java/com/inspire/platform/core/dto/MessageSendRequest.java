/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/MessageSendRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.MESSAGE_EXTRA_JSON_MAX;
import static com.inspire.platform.common.validation.ValidationConstants.MESSAGE_MAX;

@Data
public class MessageSendRequest {
    @NotNull(message = "缺少接收人")
    @Positive(message = "接收人不正确")
    private Long toUserId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = MESSAGE_MAX, message = "消息不能超过1000个字符")
    private String content;

    @Pattern(regexp = "text|image|inspire", message = "消息类型不支持")
    private String type = "text";

    @Size(max = MESSAGE_EXTRA_JSON_MAX, message = "消息扩展信息过大")
    private String extraJson;
}
