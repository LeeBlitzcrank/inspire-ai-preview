package com.inspire.platform.flink.connector;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RocketMQSource extends RichSourceFunction<String> {

    private volatile boolean running = true;
    private transient DefaultMQPushConsumer consumer;
    private final String nameServer;
    private transient BlockingQueue<String> queue;

    public RocketMQSource(String nameServer) {
        this.nameServer = nameServer;
    }

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        System.out.println("DEBUG: run() started");
        queue = new LinkedBlockingQueue<>(1000);
        consumer = new DefaultMQPushConsumer("flink_behavior_consumer");
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("topic_user_behavior", "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                try {
                    queue.offer(new String(msg.getBody()), 1, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("DEBUG: consumer started");
        System.out.println("RocketMQ Source 已启动: " + nameServer);

        // 从队列消费并在 Flink 源线程中 collect
        while (running) {
            String msg = queue.poll(1, TimeUnit.SECONDS);
            if (msg != null) {
                System.out.println("DEBUG: collecting: " + msg);
                ctx.collect(msg);
            }
        }
    }

    @Override
    public void cancel() {
        running = false;
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
