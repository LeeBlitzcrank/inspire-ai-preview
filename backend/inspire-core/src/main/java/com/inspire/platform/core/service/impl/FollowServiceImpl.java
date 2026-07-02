package com.inspire.platform.core.service.impl;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.dto.InspireVO;

import com.inspire.platform.core.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final JdbcTemplate jdbcTemplate;

    // 获取下一个ID
    private long nextId() {
        return com.inspire.platform.core.service.impl.InspireServiceImpl.nextId();
    }

    @Override
    @Transactional
    public void follow(Long myId, Long userId) {
        if (myId.equals(userId)) throw new BusinessException("不能关注自己");
        // 检查是否已关注
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND followee_id = ?",
            Integer.class, myId, userId);
        if (count != null && count > 0) throw new BusinessException("已关注该用户");
        jdbcTemplate.update("INSERT INTO user_follow(id, follower_id, followee_id) VALUES(?, ?, ?)",
            nextId(), myId, userId);
        log.info("关注: follower={}, followee={}", myId, userId);
    }

    @Override
    @Transactional
    public void unfollow(Long myId, Long userId) {
        int affected = jdbcTemplate.update(
            "DELETE FROM user_follow WHERE follower_id = ? AND followee_id = ?", myId, userId);
        if (affected == 0) throw new BusinessException("未关注该用户");
        log.info("取消关注: follower={}, followee={}", myId, userId);
    }

    @Override
    public List<Map<String, Object>> getFollowing(Long myId) {
        return jdbcTemplate.query(
            "SELECT uf.followee_id AS id, u.username, u.nickname, u.avatar FROM user_follow uf " +
            "JOIN user u ON uf.followee_id = u.id WHERE uf.follower_id = ? ORDER BY uf.create_time DESC",
            (rs, n) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("username", rs.getString("username"));
                m.put("nickname", rs.getString("nickname"));
                m.put("avatar", rs.getString("avatar"));
                return m;
            }, myId);
    }

    @Override
    public List<Map<String, Object>> getFollowers(Long myId) {
        return jdbcTemplate.query(
            "SELECT uf.follower_id AS id, u.username, u.nickname, u.avatar FROM user_follow uf " +
            "JOIN user u ON uf.follower_id = u.id WHERE uf.followee_id = ? ORDER BY uf.create_time DESC",
            (rs, n) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("username", rs.getString("username"));
                m.put("nickname", rs.getString("nickname"));
                m.put("avatar", rs.getString("avatar"));
                return m;
            }, myId);
    }

    @Override
    public List<InspireVO> getFeed(Long myId, Long followeeId, int page, int size) {
        List<Long> ids;
        if (followeeId != null) {
            ids = new ArrayList<>();
            ids.add(followeeId);
        } else {
            // 获取我关注的用户 ID 列表
            ids = jdbcTemplate.query(
                "SELECT followee_id FROM user_follow WHERE follower_id = ?",
                (rs, n) -> rs.getLong("followee_id"), myId);
        }
        if (ids.isEmpty()) return new ArrayList<>();

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id,title,img,tag,user_id,view_count,like_count,collect_count,heat,publish_city,create_time "
            + "FROM inspire_main WHERE deleted=0 AND status=1 AND user_id IN (" + placeholders + ") "
            + "ORDER BY create_time DESC LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(ids);
        params.add(size);
        params.add((page - 1) * size);

        return jdbcTemplate.query(sql, (rs, n) -> {
            InspireVO vo = new InspireVO();
            vo.setId(rs.getLong("id"));
            vo.setTitle(rs.getString("title"));
            vo.setImg(rs.getString("img"));
            vo.setTag(rs.getString("tag"));
            vo.setViewCount(rs.getLong("view_count"));
            vo.setLikeCount(rs.getInt("like_count"));
            vo.setCollectCount(rs.getInt("collect_count"));
            vo.setHeat(rs.getInt("heat"));
            vo.setPublishCity(rs.getString("publish_city"));
            vo.setCreateTime(rs.getObject("create_time", java.time.LocalDateTime.class));
            return vo;
        }, params.toArray());
    }
}
