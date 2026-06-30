package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;
@Data @Schema(description = "监控大屏数据")
public class DashboardVO {
    @Schema(description = "今日AI调用次数") private long aiCallCount;
    @Schema(description = "灵感发布总量") private long inspireTotal;
    @Schema(description = "用户总数") private long userTotal;
    @Schema(description = "分类热度 {美食:120, ...}") private Map<String, Integer> tagHeat;
    @Schema(description = "各城市发布量 {长沙:50, ...}") private Map<String, Integer> cityStats;
}
