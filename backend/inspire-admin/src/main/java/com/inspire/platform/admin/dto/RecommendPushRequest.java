/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/dto/RecommendPushRequest.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RecommendPushRequest(
        @NotNull(message = "请选择灵感")
        Long inspireId,
        @NotBlank(message = "请选择目标人群")
        String targetType,
        @Size(max = 100, message = "目标值过长")
        String targetValue,
        @NotNull(message = "请填写推送权重")
        @DecimalMin(value = "0.10", message = "权重不能小于0.10")
        @DecimalMax(value = "10.00", message = "权重不能大于10.00")
        BigDecimal weight,
        @NotBlank(message = "请选择状态")
        String status,
        String startTime,
        String endTime,
        @Size(max = 200, message = "推送原因过长")
        String reason
) {
}
