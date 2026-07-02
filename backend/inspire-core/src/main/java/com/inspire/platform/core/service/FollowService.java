package com.inspire.platform.core.service;

import com.inspire.platform.core.dto.InspireVO;

import java.util.List;
import java.util.Map;

public interface FollowService {
    void follow(Long myId, Long userId);
    void unfollow(Long myId, Long userId);
    List<Map<String, Object>> getFollowing(Long myId);
    List<Map<String, Object>> getFollowers(Long myId);
    List<InspireVO> getFeed(Long myId, Long followeeId, int page, int size);
}
