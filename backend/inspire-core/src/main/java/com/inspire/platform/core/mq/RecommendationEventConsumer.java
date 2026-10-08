/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/mq/RecommendationEventConsumer.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：异步消费推荐行为并写入事件表与用户画像
 * 维护说明：消费失败时应重试，避免推荐反馈静默丢失。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.core.service.RecommendationService;
import com.inspire.platform.core.service.RecommendationService.RecommendationEventMessage;
import com.inspire.platform.mq.constant.MqTopicConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RecommendationEventConsumer {

    private final ObjectMapper objectMapper;
    private final RecommendationService recommendationService;

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void start() {
        if (nameServer == null || nameServer.isBlank()) {
            log.info("推荐行为 MQ 未配置，事件将使用同步兜底写入");
            return;
        }
        try {
            consumer = new DefaultMQPushConsumer("consumer_recommend_event");
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(MqTopicConstants.TOPIC_RECOMMEND_EVENT, "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                for (MessageExt message : messages) {
                    try {
                        RecommendationEventMessage event = objectMapper.readValue(
                                new String(message.getBody(), StandardCharsets.UTF_8),
                                RecommendationEventMessage.class);
                        recommendationService.persistEvent(event);
                    } catch (Exception e) {
                        log.warn("推荐行为消费失败: {}", e.getMessage());
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            log.info("推荐行为消费者已启动: topic={}", MqTopicConstants.TOPIC_RECOMMEND_EVENT);
        } catch (Exception e) {
            log.warn("推荐行为消费者启动失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) consumer.shutdown();
    }
}
