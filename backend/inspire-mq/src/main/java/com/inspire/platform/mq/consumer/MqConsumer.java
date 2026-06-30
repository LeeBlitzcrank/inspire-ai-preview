package com.inspire.platform.mq.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MqConsumer {

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    private final List<Object> consumers = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (nameServer.isEmpty()) {
            log.info("RocketMQ未配置，跳过消费者启动");
            return;
        }
        startOne("consumer_register", "topic_user_register");
        startOne("consumer_behavior", "topic_user_behavior");
        startOne("consumer_publish", "topic_inspire_publish");
        log.info("MQ消费者已启动: {}", consumers.size() + "个");
    }

    private void startOne(String group, String topic) {
        try {
            Class<?> clz = Class.forName("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer");
            Object consumer = clz.getConstructor(String.class).newInstance(group);

            clz.getMethod("setNamesrvAddr", String.class).invoke(consumer, nameServer);
            clz.getMethod("subscribe", String.class, String.class).invoke(consumer, topic, "*");

            // 创建 MessageListenerConcurrently 动态代理
            Class<?> listenerIf = Class.forName("org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently");
            Object listener = Proxy.newProxyInstance(
                    listenerIf.getClassLoader(),
                    new Class<?>[]{listenerIf},
                    (proxy, method, args) -> {
                        if ("consumeMessage".equals(method.getName())) {
                            @SuppressWarnings("unchecked")
                            List<Object> msgs = (List<Object>) args[0];
                            for (Object msg : msgs) {
                                byte[] body = (byte[]) msg.getClass().getMethod("getBody").invoke(msg);
                                log.info("【{}】收到消息: {}", topic, new String(body));
                            }
                            Class<?> stClz = Class.forName("org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus");
                            return stClz.getField("CONSUME_SUCCESS").get(null);
                        }
                        return null;
                    });

            clz.getMethod("registerMessageListener", listenerIf).invoke(consumer, listener);
            clz.getMethod("start").invoke(consumer);
            consumers.add(consumer);
            log.info("MQ消费者[{}]启动: topic={}", group, topic);
        } catch (ClassNotFoundException e) {
            log.warn("RocketMQ客户端未安装，跳过消费者[{}]: {}", group, topic);
        } catch (Exception e) {
            log.warn("MQ消费者[{}]启动失败: {}", group, e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        for (Object c : consumers) {
            try { c.getClass().getMethod("shutdown").invoke(c); } catch (Exception ignored) {}
        }
    }
}
