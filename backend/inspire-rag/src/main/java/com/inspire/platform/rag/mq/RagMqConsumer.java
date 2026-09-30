/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/mq/RagMqConsumer.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.service.RagIndexService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RagMqConsumer {

    private static final String TOPIC = "topic_inspire_rag_sync";

    private final ObjectMapper objectMapper;
    private final RagIndexService indexService;
    private final RagProperties properties;

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    private DefaultMQPushConsumer consumer;

    public RagMqConsumer(ObjectMapper objectMapper,
                         RagIndexService indexService,
                         RagProperties properties) {
        this.objectMapper = objectMapper;
        this.indexService = indexService;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled() || nameServer == null || nameServer.isBlank()) {
            log.info("RAG MQ 未启用或 RocketMQ 未配置");
            return;
        }
        try {
            consumer = new DefaultMQPushConsumer("consumer_rag_sync");
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(TOPIC, "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                for (MessageExt message : messages) {
                    handle(new String(message.getBody(), StandardCharsets.UTF_8));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            log.info("RAG MQ 消费者已启动: topic={}", TOPIC);
        } catch (Exception e) {
            log.warn("RAG MQ 消费者启动失败: {}", e.getMessage());
        }
    }

    private void handle(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            long inspireId = json.path("inspireId").asLong(0);
            if (inspireId <= 0) return;
            String operation = json.path("operation").asText("UPSERT");
            if ("DELETE".equalsIgnoreCase(operation)) {
                indexService.deleteInspire(inspireId);
            } else {
                indexService.indexInspire(inspireId);
            }
        } catch (Exception e) {
            log.warn("RAG MQ 消息处理失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
