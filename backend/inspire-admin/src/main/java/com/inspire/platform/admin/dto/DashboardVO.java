/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/dto/DashboardVO.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;
@Data @Schema(description = "监控大屏数据")
public class DashboardVO {
    @Schema(description = "今日AI调用次数") private long aiCallCount;
    @Schema(description = "灵感发布总量") private long inspireTotal;
    @Schema(description = "用户总数") private long userTotal;

    @Schema(description = "日活用户（今日有操作的用户数）") private long dau;
    @Schema(description = "今日发布灵感数") private long todayPublish;
    @Schema(description = "今日注册数") private long todayRegister;
    @Schema(description = "今日总点赞数") private long todayLikes;
    @Schema(description = "今日总收藏数") private long todayCollects;
    @Schema(description = "累计总点赞数") private long totalLikes;
    @Schema(description = "累计总收藏数") private long totalCollects;

    @Schema(description = "分类热度 {美食:120, ...}") private Map<String, Integer> tagHeat;
    @Schema(description = "各城市发布量 {长沙:50, ...}") private Map<String, Integer> cityStats;
}
