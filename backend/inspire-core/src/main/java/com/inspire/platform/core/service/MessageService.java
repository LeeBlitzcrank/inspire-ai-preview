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
