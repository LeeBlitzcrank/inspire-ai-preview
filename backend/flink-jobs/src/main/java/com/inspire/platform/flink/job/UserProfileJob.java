package com.inspire.platform.flink.job;
import com.inspire.platform.flink.model.UserBehavior;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import redis.clients.jedis.Jedis;

public class UserProfileJob {
    public static void build(StreamExecutionEnvironment env, DataStream<String> source) {
        source
            .map((MapFunction<String, UserBehavior>) json -> {
                try { return UserBehavior.fromJson(json); }
                catch (Exception e) { return null; }
            }).name("up-parse")
            .filter(b -> b != null && b.tag != null && !b.tag.isEmpty()).name("up-filter")
            .map((MapFunction<UserBehavior, String>) b -> {
                int w = switch (b.type) {
                    case "publish" -> 10; case "collect" -> 8;
                    case "like" -> 5; case "view" -> 3;
                    case "search" -> 2; default -> 1;
                };
                try {
                    String host = System.getenv().getOrDefault("INSPIRE_REDIS_HOST", "localhost");
                    int port = Integer.parseInt(System.getenv().getOrDefault("INSPIRE_REDIS_PORT", "6379"));
                    Jedis jedis = new Jedis(host, port);
                    String password = System.getenv().getOrDefault("INSPIRE_REDIS_PASSWORD", "");
                    if (!password.isBlank()) jedis.auth(password);
                    jedis.hincrBy("profile:" + b.userId, b.tag, w);
                    jedis.expire("profile:" + b.userId, 86400);
                    jedis.close();
                } catch (Exception e) { System.out.println("DEBUG: Redis error: " + e.getMessage()); }
                return String.format("userId=%d, tag=%s, +%d", b.userId, b.tag, w);
            }).name("up-redis")
            .print();
    }
}
