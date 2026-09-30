/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/SeriesSaveRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系列创建/修改参数")
public class SeriesSaveRequest {
    @NotBlank(message = "请输入系列名称")
    @Size(max = 50, message = "系列名称不能超过50个字")
    private String name;

    @Size(max = 300, message = "系列描述不能超过300个字")
    private String description;
}
