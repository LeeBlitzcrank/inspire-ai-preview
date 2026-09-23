package com.inspire.platform.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "系列文章排序参数")
public class SeriesOrderRequest {
    @NotEmpty(message = "系列文章不能为空")
    private List<Long> articleIds;
}
