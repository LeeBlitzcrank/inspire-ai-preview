package com.inspire.platform.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminConfigUpdateRequest {
    @NotNull(message = "配置ID不能为空")
    @Positive(message = "配置ID不正确")
    private Integer id;

    @Size(max = 500, message = "配置值不能超过500个字符")
    private String value;
}
