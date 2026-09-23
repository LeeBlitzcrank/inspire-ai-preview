package com.inspire.platform.flink.job;
import com.inspire.platform.flink.model.UserBehavior;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import redis.clients.jedis.Jedis;

public class HotAggregationJob {
    public static void build(StreamExecutionEnvironment env, DataStream<String> source) {
        source
            .map((MapFunction<String, UserBehavior>) json -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> map = m.readValue(json, java.util.Map.class);
                    UserBehavior b = new UserBehavior();
                    b.userId = ((Number) map.getOrDefault("userId", 0L)).longValue();
                    b.type = (String) map.getOrDefault("type", "");
                    b.tag = (String) map.getOrDefault("tag", "");
                    b.city = (String) map.getOrDefault("city", "");
                    return b;
                } catch (Exception e) { return null; }
            }).name("hot-parse")
            .filter(b -> b != null && b.city != null && !b.city.isEmpty()).name("hot-filter")
            .map((MapFunction<UserBehavior, String>) b -> {
                String key = "hot:" + b.city + ":" + b.tag;
                try {
                    String host = System.getenv().getOrDefault("INSPIRE_REDIS_HOST", "localhost");
                    int port = Integer.parseInt(System.getenv().getOrDefault("INSPIRE_REDIS_PORT", "6379"));
                    Jedis jedis = new Jedis(host, port);
                    String password = System.getenv().getOrDefault("INSPIRE_REDIS_PASSWORD", "");
                    if (!password.isBlank()) jedis.auth(password);
                    jedis.incr(key);
                    jedis.expire(key, 600);
                    jedis.close();
                } catch (Exception e) { System.out.println("DEBUG: Redis error: " + e.getMessage()); }
                return String.format("{city=%s, tag=%s}", b.city, b.tag);
            }).name("hot-redis")
            .print();
    }
}
