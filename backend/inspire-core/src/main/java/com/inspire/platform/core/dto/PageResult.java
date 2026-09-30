/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/PageResult.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
