package com.inspire.platform.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> {
    @Schema(description = "数据列表")
    private List<T> records;
    @Schema(description = "总数")
    private long total;
    @Schema(description = "下一页游标")
    private String nextCursor;
    @Schema(description = "是否还有下一页")
    private boolean hasMore;

    public PageResult(List<T> records, long total) {
        this(records, total, null, false);
    }

    public PageResult(List<T> records, long total, String nextCursor, boolean hasMore) {
        this.records = records;
        this.total = total;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }
}
