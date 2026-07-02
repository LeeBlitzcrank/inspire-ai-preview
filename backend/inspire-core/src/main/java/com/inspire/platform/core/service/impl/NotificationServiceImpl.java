package com.inspire.platform.core.service.impl;

import com.inspire.platform.core.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void notify(Long userId, String type, Long actorId, String actorName,
                       String content, Long targetId, String targetTitle) {
        // 自己触发的不通知
        if (userId.equals(actorId)) return;
        long id = nextId();
        jdbcTemplate.update(
            "INSERT INTO user_notification(id, user_id, type, actor_id, actor_name, content, target_id, target_title) VALUES(?,?,?,?,?,?,?,?)",
            id, userId, type, actorId, actorName, content, targetId, targetTitle
        );
        log.debug("通知已创建: type={}, to={}, from={}", type, userId, actorId);
    }

    @Override
    public List<Map<String, Object>> list(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return jdbcTemplate.query(
            "SELECT id, type, actor_id, actor_name, content, target_id, target_title, is_read, create_time " +
            "FROM user_notification WHERE user_id = ? AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT ? OFFSET ?",
            (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("type", rs.getString("type"));
                m.put("actorId", String.valueOf(rs.getLong("actor_id")));
                m.put("actorName", rs.getString("actor_name"));
                m.put("content", rs.getString("content"));
                m.put("targetId", String.valueOf(rs.getLong("target_id")));
                m.put("targetTitle", rs.getString("target_title"));
                m.put("isRead", rs.getInt("is_read"));
                m.put("createTime", rs.getObject("create_time", java.time.LocalDateTime.class).toString());
                return m;
            },
            userId, size, offset
        );
    }

    @Override
    public long unreadCount(Long userId) {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_notification WHERE user_id = ? AND deleted = 0 AND is_read = 0",
            Integer.class, userId);
        return cnt != null ? cnt : 0L;
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        if (notificationId != null) {
            jdbcTemplate.update(
                "UPDATE user_notification SET is_read = 1 WHERE id = ? AND user_id = ?",
                notificationId, userId);
        } else {
            jdbcTemplate.update(
                "UPDATE user_notification SET is_read = 1 WHERE user_id = ? AND deleted = 0",
                userId);
        }
    }

    private static long seq = 0L, lastTs = -1L;
    private static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) ts = lastTs;
        if (ts == lastTs) { seq = (seq + 1) & 0xFFF; if (seq == 0) ts++; }
        else seq = 0;
        lastTs = ts;
        return ((ts - 1735689600000L) << 22) | (1L << 12) | 1L;
    }
}
