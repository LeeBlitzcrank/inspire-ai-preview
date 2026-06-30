package com.inspire.platform.mq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class MqProducer {

    private final ObjectMapper objectMapper;
    private final String nameServer;
    private Object producer;
    private boolean available = false;

    public MqProducer(ObjectMapper objectMapper,
                      @Value("${rocketmq.name-server:}") String nameServer) {
        this.objectMapper = objectMapper;
        this.nameServer = nameServer;
    }

    @PostConstruct
    public void init() {
        if (nameServer.isEmpty()) {
            log.info("RocketMQ未配置，消息仅打印日志");
            return;
        }
        try {
            Class<?> clz = Class.forName("org.apache.rocketmq.client.producer.DefaultMQProducer");
            producer = clz.getConstructor(String.class).newInstance("inspire_producer_group");
            clz.getMethod("setNamesrvAddr", String.class).invoke(producer, nameServer);
            clz.getMethod("start").invoke(producer);
            available = true;
            log.info("RocketMQ生产者启动成功: namesrv={}", nameServer);
        } catch (ClassNotFoundException e) {
            log.warn("RocketMQ客户端未安装");
        } catch (Exception e) {
            log.warn("RocketMQ生产者启动失败: {}", e.getMessage());
        }
    }

    public void send(String topic, Object data) {
        if (!available) {
            log.info("【MQ消息】topic={}, data={}", topic, safeJson(data));
            return;
        }
        try {
            Class<?> msgClz = Class.forName("org.apache.rocketmq.common.message.Message");
            Object msg = msgClz.getConstructor(String.class, byte[].class)
                    .newInstance(topic, safeJson(data).getBytes("UTF-8"));
            producer.getClass().getMethod("send", msgClz).invoke(producer, msg);
            log.debug("MQ发送成功: topic={}", topic);
        } catch (Exception e) {
            log.warn("MQ发送失败: {} - {}", e.getClass().getSimpleName(),
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private String safeJson(Object data) {
        try { return objectMapper.writeValueAsString(data); }
        catch (Exception e) { return String.valueOf(data); }
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            try { producer.getClass().getMethod("shutdown").invoke(producer); }
            catch (Exception ignored) {}
        }
    }
}
