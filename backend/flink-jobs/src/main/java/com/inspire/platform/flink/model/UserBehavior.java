/**
 * 文件：backend/flink-jobs/src/main/java/com/inspire/platform/flink/model/UserBehavior.java
 * 所属模块：实时计算模块，负责热点聚合和用户行为计算
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.flink.model;
import java.util.Map;
public class UserBehavior {
    public long userId; public String type; public Long inspireId;
    public String keyword; public String tag; public String city;
    public long timestamp;

    public static UserBehavior fromJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = m.readValue(json, Map.class);
            UserBehavior b = new UserBehavior();
            b.userId = ((Number) map.getOrDefault("userId", 0L)).longValue();
            b.type = (String) map.getOrDefault("type", "");
            Object id = map.get("inspireId");
            b.inspireId = id != null ? ((Number) id).longValue() : null;
            b.keyword = (String) map.getOrDefault("keyword", "");
            b.tag = (String) map.getOrDefault("tag", "");
            b.city = (String) map.getOrDefault("city", "");
            b.timestamp = System.currentTimeMillis();
            return b;
        } catch (Exception e) { return null; }
    }
}
