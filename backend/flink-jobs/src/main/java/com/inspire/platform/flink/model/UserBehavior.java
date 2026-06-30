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
