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
