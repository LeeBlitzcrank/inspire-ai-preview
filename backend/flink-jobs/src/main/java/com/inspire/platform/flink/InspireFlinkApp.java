package com.inspire.platform.flink;

import com.inspire.platform.flink.connector.RocketMQSource;
import com.inspire.platform.flink.job.HotAggregationJob;
import com.inspire.platform.flink.job.UserProfileJob;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class InspireFlinkApp {

    public static void main(String[] args) throws Exception {
        String mode = getArg(args, "--mode", "local");
        String source = getArg(args, "--source", "socket");
        String host = getArg(args, "--host", "localhost");
        int port = Integer.parseInt(getArg(args, "--port", "9999"));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        if ("rocketmq".equals(source)) {
            org.apache.flink.streaming.api.datastream.DataStream<String> stream =
                env.addSource(new RocketMQSource(host + ":" + port));
            System.out.println("===== Flink 实时计算已启动 (数据源: RocketMQ " + host + ":" + port + ") =====");
            UserProfileJob.build(env, stream);
            HotAggregationJob.build(env, stream);
        } else {
            var socketSource = env.socketTextStream(host, port);
            System.out.println("===== Flink 实时计算已启动 (数据源: Socket " + host + ":" + port + ") =====");
            UserProfileJob.build(env, socketSource);
            HotAggregationJob.build(env, socketSource);
        }

        env.execute("Inspire Flink Jobs");
    }

    private static String getArg(String[] args, String key, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) return args[i + 1];
        }
        return def;
    }
}
