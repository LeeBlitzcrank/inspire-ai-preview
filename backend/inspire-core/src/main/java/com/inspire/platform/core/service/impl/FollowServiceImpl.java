package com.inspire.platform.core.service.impl;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.dto.InspireVO;
import com.inspire.platform.core.service.FeedService;
import com.inspire.platform.core.service.FollowService;
import com.inspire.platform.core.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;
    private final FeedService feedService;

    // 获取下一个ID
    private long nextId() {
        return com.inspire.platform.core.service.impl.InspireServiceImpl.nextId();
    }

    @Override
    @Transactional
    public void follow(Long myId, Long userId) {
        if (myId.equals(userId)) {
            throw new BusinessException("不能关注自己");
        }
        // 检查是否已关注
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND followee_id = ?",
            Integer.class, myId, userId);
        if (count != null && count > 0) {
            throw new BusinessException("已关注该用户");
        }
        jdbcTemplate.update("INSERT INTO user_follow(id, follower_id, followee_id) VALUES(?, ?, ?)",
            nextId(), myId, userId);
        feedService.backfillFollower(myId, userId, 100);
        log.info("关注: follower={}, followee={}", myId, userId);
        try {
            String myName = jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, myId);
            notificationService.notify(userId, "follow", myId,
                myName != null ? myName : String.valueOf(myId), "关注了你", null, null);
        } catch (Exception e) { log.warn("关注通知发送失败", e); }    }

    @Override
    @Transactional
    public void unfollow(Long myId, Long userId) {
        int affected = jdbcTemplate.update(
            "DELETE FROM user_follow WHERE follower_id = ? AND followee_id = ?", myId, userId);
        if (affected == 0) {
            throw new BusinessException("未关注该用户");
        }
        feedService.removeFollower(myId, userId);
        log.info("取消关注: follower={}, followee={}", myId, userId);
    }

    @Override
    @Transactional
    public void setSpecial(Long myId, Long userId, boolean special) {
        int affected = jdbcTemplate.update(
                "UPDATE user_follow SET special = ? WHERE follower_id = ? AND followee_id = ?",
                special ? 1 : 0, myId, userId);
        if (affected == 0) {
            throw new BusinessException("请先关注该用户");
        }
    }

    @Override
    public List<Map<String, Object>> getFollowing(Long myId) {
        return jdbcTemplate.query(
            "SELECT uf.followee_id AS id, u.nickname, u.avatar, uf.special FROM user_follow uf " +
            "JOIN user u ON uf.followee_id = u.id WHERE uf.follower_id = ? ORDER BY uf.create_time DESC",
            (rs, n) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("nickname", rs.getString("nickname"));
                m.put("avatar", rs.getString("avatar"));
                m.put("special", rs.getInt("special") == 1);
                return m;
            }, myId);
    }

    @Override
    public List<Map<String, Object>> getFollowers(Long myId) {
        return jdbcTemplate.query(
            "SELECT uf.follower_id AS id, u.nickname, u.avatar FROM user_follow uf " +
            "JOIN user u ON uf.follower_id = u.id WHERE uf.followee_id = ? ORDER BY uf.create_time DESC",
            (rs, n) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("nickname", rs.getString("nickname"));
                m.put("avatar", rs.getString("avatar"));
                return m;
            }, myId);
    }

    @Override
    public List<InspireVO> getFeed(Long myId, Long followeeId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, page - 1) * safeSize;
        List<Object> params = new ArrayList<>();
        params.add(myId);
        String authorFilter = "";
        if (followeeId != null) {
            authorFilter = " AND f.author_id = ? ";
            params.add(followeeId);
        }
        params.add(safeSize);
        params.add(offset);

        String sql = "SELECT i.id,i.title,i.img,i.tag,i.user_id,i.view_count,i.like_count,"
                + "i.collect_count,i.heat,i.publish_city,i.create_time "
                + "FROM user_feed f JOIN inspire_main i ON i.id = f.inspire_id "
                + "WHERE f.user_id = ? AND i.deleted = 0 AND i.status = 1 "
                + authorFilter
                + "ORDER BY f.create_time DESC, f.id DESC LIMIT ? OFFSET ?";

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
