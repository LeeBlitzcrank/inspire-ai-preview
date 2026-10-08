/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/dto/RecommendConfigUpdateRequest.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendConfigUpdateRequest(
        @NotBlank(message = "缺少配置项")
        @Size(max = 80, message = "配置项过长")
        String configKey,
        @NotBlank(message = "缺少配置值")
        @Size(max = 500, message = "配置值过长")
        String configValue
) {
}
