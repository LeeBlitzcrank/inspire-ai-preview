/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/MessageService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;

import java.util.List;

public interface MessageService {
    Message sendMessage(Long fromUserId, Long toUserId, String content);
    Message sendMessage(Long fromUserId, Long toUserId, String content, String type, String extraJson);
    List<MessageConversation> getConversations(Long userId);
    List<Message> getMessages(Long userId, Long conversationId, int page, int size);
    int markAsRead(Long userId, Long conversationId);
    int unreadCount(Long userId);
    void deleteConversation(Long userId, Long conversationId);
    void deleteAllConversations(Long userId);
    MessageConversation startConversation(Long userId, Long toUserId);
    void recallMessage(Long userId, Long conversationId, Long messageId);
}
