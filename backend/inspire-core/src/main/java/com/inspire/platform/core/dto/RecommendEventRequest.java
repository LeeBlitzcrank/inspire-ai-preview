/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/RecommendEventRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecommendEventRequest(
        @NotNull(message = "缺少灵感ID")
        Long inspireId,
        @NotBlank(message = "缺少推荐行为类型")
        @Size(max = 20, message = "推荐行为类型过长")
        String eventType,
        @Size(max = 40, message = "推荐原因过长")
        String reasonCode,
        @Size(max = 40, message = "实验ID过长")
        String experimentId,
        @Size(max = 20, message = "实验分组过长")
        String variant,
        Integer durationMs,
        Long pushId
) {
}
