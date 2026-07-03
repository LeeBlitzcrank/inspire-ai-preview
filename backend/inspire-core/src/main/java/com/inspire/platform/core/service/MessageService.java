package com.inspire.platform.core.service;

import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;
import java.util.List;
import java.util.Map;

public interface MessageService {
    Message sendMessage(Long fromUserId, Long toUserId, String content);
    List<MessageConversation> getConversations(Long userId);
    List<Message> getMessages(Long userId, Long conversationId, int page, int size);
    int markAsRead(Long userId, Long conversationId);
    int unreadCount(Long userId);
    void deleteConversation(Long userId, Long conversationId);
    void deleteAllConversations(Long userId);
    MessageConversation startConversation(Long userId, Long toUserId);
}
