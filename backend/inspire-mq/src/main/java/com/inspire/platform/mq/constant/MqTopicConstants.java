/**
 * 文件：backend/inspire-mq/src/main/java/com/inspire/platform/mq/constant/MqTopicConstants.java
 * 所属模块：消息队列公共模块，负责生产者、消费者和积压指标
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.mq.constant;

public class MqTopicConstants {
    public static final String TOPIC_USER_REGISTER = "topic_user_register";
    public static final String TOPIC_USER_BEHAVIOR = "topic_user_behavior";
    public static final String TOPIC_INSPIRE_PUBLISH = "topic_inspire_publish";
    public static final String TOPIC_INSPIRE_RAG_SYNC = "topic_inspire_rag_sync";
    public static final String TOPIC_RECOMMEND_EVENT = "topic_recommend_event";
}
