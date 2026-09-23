package com.inspire.platform.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "系列文章操作参数")
public class SeriesArticleRequest {
    @NotNull(message = "缺少灵感ID")
    private Long inspireId;
}
