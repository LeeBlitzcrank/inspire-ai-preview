/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/NotificationService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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

    /** 目标内容删除后失效关联通知 */
    void invalidateTarget(String targetType, Long targetId);
}
