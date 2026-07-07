package com.inspire.platform.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> {
    @Schema(description = "数据列表")
    private List<T> records;
    @Schema(description = "总数")
    private long total;
}
