package com.inspire.platform.core.service;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /** 创建通知 */
    void notify(Long userId, String type, Long actorId, String actorName,
                String content, Long targetId, String targetTitle);

    /** 获取我的通知列表 */
    List<Map<String, Object>> list(Long userId, int page, int size);

    /** 未读数量 */
    long unreadCount(Long userId);

    /** 标记已读 */
    void markRead(Long userId, Long notificationId);
}
