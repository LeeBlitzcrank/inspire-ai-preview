/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/AiHistoryService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import java.util.List;
import java.util.Map;

public interface AiHistoryService {
    List<Map<String, Object>> list(Long userId, int limit);
    Map<String, Object> save(Long userId, Map<String, Object> body);
    void selectVariant(Long userId, Long id, Integer selectedIndex, String selectedTitle);
    void delete(Long userId, Long id);
}
