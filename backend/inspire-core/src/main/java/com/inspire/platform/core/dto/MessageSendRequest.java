package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageSendRequest {
    @NotNull(message = "缺少接收人")
    private Long toUserId;
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private String type = "text";
    private String extraJson;
}
