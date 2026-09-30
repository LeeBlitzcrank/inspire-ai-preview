/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/FollowService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import com.inspire.platform.core.dto.InspireVO;

import java.util.List;
import java.util.Map;

public interface FollowService {
    void follow(Long myId, Long userId);
    void unfollow(Long myId, Long userId);
    void setSpecial(Long myId, Long userId, boolean special);
    List<Map<String, Object>> getFollowing(Long myId);
    List<Map<String, Object>> getFollowers(Long myId);
    List<InspireVO> getFeed(Long myId, Long followeeId, int page, int size);
}
