package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;
import com.inspire.platform.core.entity.InspireComment;
import com.inspire.platform.core.mapper.InspireCommentMapper;
import com.inspire.platform.core.service.CommentService;
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
public class CommentServiceImpl implements CommentService {

    private final InspireCommentMapper commentMapper;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    private static long seq = 0L, lastTs = -1L;

    @Override
    public Page<CommentVO> listByInspireId(Long inspireId, Long userId, int page, int size, String sort) {
        boolean hotSort = "hot".equalsIgnoreCase(sort);
        Page<InspireComment> pg;
        ShardContext.setByInspireId(inspireId);
        try {
            LambdaQueryWrapper<InspireComment> query = new LambdaQueryWrapper<InspireComment>()
                    .eq(InspireComment::getInspireId, inspireId)
                    .eq(InspireComment::getDeleted, 0);
            if (hotSort) {
                query.orderByDesc(InspireComment::getLikeCount)
                        .orderByDesc(InspireComment::getCreateTime);
            } else {
                query.orderByDesc(InspireComment::getCreateTime);
            }
            pg = commentMapper.selectPage(
                    new Page<>(page, size),
                    query);
        } finally {
            ShardContext.clear();
        }

        Page<CommentVO> voPage = new Page<>(pg.getCurrent(), pg.getSize(), pg.getTotal());
        List<CommentVO> records = pg.getRecords().stream().map(this::toVO).toList();
        fillLikedState(records, userId);
        voPage.setRecords(records);
        return voPage;
    }

    private CommentVO toVO(InspireComment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setInspireId(c.getInspireId());
        vo.setUserId(c.getUserId());
        vo.setUsername(c.getUsername());
        vo.setAvatar(c.getAvatar());
        vo.setNickname(c.getUsername());
        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setReplyUserId(c.getReplyUserId());
        vo.setReplyUsername(c.getReplyUsername());
        vo.setLikeCount(c.getLikeCount() == null ? 0 : c.getLikeCount());
        vo.setLiked(false);
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private void fillLikedState(List<CommentVO> records, Long userId) {
        if (userId == null || records.isEmpty()) return;
        List<Object> args = new ArrayList<>();
        args.add(userId);
        records.forEach(item -> args.add(item.getId()));
        String placeholders = String.join(",", Collections.nCopies(records.size(), "?"));
        try {
            Set<Long> likedIds = new HashSet<>(jdbcTemplate.queryForList(
                    "SELECT comment_id FROM comment_like_" + Math.floorMod(userId, 10)
                            + " WHERE user_id = ? AND comment_id IN (" + placeholders + ")",
                    Long.class, args.toArray()));
            records.forEach(item -> item.setLiked(likedIds.contains(item.getId())));
        } catch (Exception e) {
            log.warn("评论点赞状态查询失败: userId={}", userId, e);
        }
    }

    @Override
    @Transactional
    public void create(Long userId, CommentCreateRequest request) {
        String nickname = request.getUsername();
        String avatar = request.getAvatar();
        try {
            Map<String, Object> profile = jdbcTemplate.queryForMap(
                    "SELECT nickname, avatar FROM user WHERE id=?", userId);
            nickname = java.util.Objects.toString(profile.get("nickname"), String.valueOf(userId));
            avatar = java.util.Objects.toString(profile.get("avatar"), "");
        } catch (Exception e) {
            nickname = nickname != null ? nickname : String.valueOf(userId);
            avatar = avatar != null ? avatar : "";
        }

        InspireComment c = new InspireComment();
        c.setId(nextId());
        c.setInspireId(request.getInspireId());
        c.setUserId(userId);
        c.setUsername(nickname);
        c.setAvatar(avatar);
        c.setContent(request.getContent());
        c.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        c.setReplyUserId(request.getReplyUserId() != null ? request.getReplyUserId() : 0L);
        c.setReplyUsername(request.getReplyUsername() != null ? request.getReplyUsername() : "");
        c.setLikeCount(0);
        c.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        int commentShard = (int) Math.floorMod(request.getInspireId(), 10);
        ShardContext.setByInspireId(request.getInspireId());
        try {
            commentMapper.insert(c);
        } finally {
            ShardContext.clear();
        }
        log.info("评论创建: id={}, inspireId={}, userId={}, parentId={}",
                c.getId(), request.getInspireId(), userId, c.getParentId());

        // 通知灵感作者或被回复者
        try {
            String myName = jdbcTemplate.queryForObject(
                "SELECT nickname FROM user WHERE id=?", String.class, userId);
            if (myName == null) {
                myName = String.valueOf(userId);
            }
            String title = jdbcTemplate.queryForObject(
                "SELECT title FROM inspire_main WHERE id=?", String.class, request.getInspireId());
            if (title != null && title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }

            if (request.getParentId() != null && request.getParentId() > 0L) {
                // 回复评论 → 通知被回复者
                String replyOwnerId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM inspire_comment_" + commentShard + " WHERE id=?",
                    String.class, request.getParentId());
                if (replyOwnerId != null) {
                    notificationService.notify(Long.parseLong(replyOwnerId), "reply", userId,
                        myName, "回复了你的评论: " + request.getContent(),
                        request.getInspireId(), title);
                }
            } else {
                // 评论灵感 → 通知灵感作者
                String authorId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM inspire_main WHERE id=?", String.class, request.getInspireId());
                if (authorId != null) {
                    notificationService.notify(Long.parseLong(authorId), "comment", userId,
                        myName, "评论了你的灵感: " + request.getContent(),
                        request.getInspireId(), title);
                }
            }
        } catch (Exception e) {
            log.warn("评论通知发送失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteById(Long inspireId, Long commentId, Long userId) {
        ShardContext.setByInspireId(inspireId);
        try {
            commentMapper.delete(new LambdaQueryWrapper<InspireComment>()
                    .eq(InspireComment::getId, commentId)
                    .eq(InspireComment::getUserId, userId));
            commentMapper.delete(new LambdaQueryWrapper<InspireComment>()
                    .eq(InspireComment::getParentId, commentId));
        } finally {
            ShardContext.clear();
        }
        log.info("评论删除: commentId={}, userId={}", commentId, userId);
    }

    @Override
    @Transactional
    public boolean like(Long userId, Long inspireId, Long commentId) {
        int userShard = Math.floorMod(userId, 10);
        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO comment_like_" + userShard
                        + "(id, user_id, comment_id, create_time) VALUES(?,?,?,?)",
                com.inspire.platform.core.service.impl.InspireServiceImpl.nextId(),
                userId, commentId, java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        if (inserted == 0) return false;

        int commentShard = Math.floorMod(inspireId, 10);
        int updated = jdbcTemplate.update(
                "UPDATE inspire_comment_" + commentShard
                        + " SET like_count = COALESCE(like_count, 0) + 1"
                        + " WHERE id = ? AND inspire_id = ? AND deleted = 0",
                commentId, inspireId);
        if (updated == 0) {
            jdbcTemplate.update(
                    "DELETE FROM comment_like_" + userShard + " WHERE user_id = ? AND comment_id = ?",
                    userId, commentId);
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public boolean unlike(Long userId, Long inspireId, Long commentId) {
        int userShard = Math.floorMod(userId, 10);
        int deleted = jdbcTemplate.update(
                "DELETE FROM comment_like_" + userShard + " WHERE user_id = ? AND comment_id = ?",
                userId, commentId);
        if (deleted == 0) return false;

        int commentShard = Math.floorMod(inspireId, 10);
        jdbcTemplate.update(
                "UPDATE inspire_comment_" + commentShard
                        + " SET like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)"
                        + " WHERE id = ? AND inspire_id = ? AND deleted = 0",
                commentId, inspireId);
        return true;
    }

    private static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            ts = lastTs;
        }
        if (ts == lastTs) { seq = (seq + 1) & 0xFFF; if (seq == 0) {
            ts++;
        }
        }
        else {
            seq = 0;
        }
        lastTs = ts;
        return ((ts - 1735689600000L) << 22) | (1L << 12) | 1L;
    }
}
