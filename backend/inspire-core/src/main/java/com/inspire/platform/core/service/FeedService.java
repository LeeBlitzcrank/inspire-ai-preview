package com.inspire.platform.core.service;

import com.inspire.platform.core.service.impl.InspireServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 关注流收件箱：写入时扇出，读取时不再扫描关注关系和 IN 列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final JdbcTemplate jdbcTemplate;

    public void fanout(Long inspireId, Long authorId, LocalDateTime createTime) {
        List<Long> followers = jdbcTemplate.queryForList(
                "SELECT follower_id FROM user_follow WHERE followee_id = ?",
                Long.class, authorId);
        if (followers.isEmpty()) return;
        List<Object[]> batch = new ArrayList<>(followers.size());
        for (Long followerId : followers) {
            batch.add(new Object[]{
                    InspireServiceImpl.nextId(), followerId, inspireId, authorId,
                    createTime == null ? LocalDateTime.now() : createTime
            });
        }
        jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO user_feed(id,user_id,inspire_id,author_id,create_time) "
                        + "VALUES(?,?,?,?,?)",
                batch);
        log.debug("关注流扇出完成: inspireId={}, followers={}", inspireId, followers.size());
    }

    public void backfillFollower(Long followerId, Long followeeId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<Object[]> rows = jdbcTemplate.query(
                "SELECT id, create_time FROM inspire_main "
                        + "WHERE user_id = ? AND status = 1 AND deleted = 0 "
                        + "ORDER BY create_time DESC, id DESC LIMIT ?",
                (rs, rowNum) -> new Object[]{
                        InspireServiceImpl.nextId(), followerId, rs.getLong("id"), followeeId,
                        rs.getObject("create_time", LocalDateTime.class)
                },
                followeeId, safeLimit);
        if (!rows.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO user_feed(id,user_id,inspire_id,author_id,create_time) "
                            + "VALUES(?,?,?,?,?)",
                    rows);
        }
    }

    public void removeFollower(Long followerId, Long followeeId) {
        jdbcTemplate.update(
                "DELETE FROM user_feed WHERE user_id = ? AND author_id = ?",
                followerId, followeeId);
    }

    public void deleteInspire(Long inspireId) {
        jdbcTemplate.update("DELETE FROM user_feed WHERE inspire_id = ?", inspireId);
    }

    public void rebuildForUser(Long userId, int limit) {
        jdbcTemplate.update("DELETE FROM user_feed WHERE user_id = ?", userId);
        List<Long> followees = jdbcTemplate.queryForList(
                "SELECT followee_id FROM user_follow WHERE follower_id = ?",
                Long.class, userId);
        for (Long followeeId : followees) {
            backfillFollower(userId, followeeId, limit);
        }
    }
}
