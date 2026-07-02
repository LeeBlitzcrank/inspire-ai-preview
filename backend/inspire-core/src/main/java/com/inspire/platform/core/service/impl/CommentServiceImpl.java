package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final InspireCommentMapper commentMapper;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    private static long seq = 0L, lastTs = -1L;

    @Override
    public Page<CommentVO> listByInspireId(Long inspireId, int page, int size) {
        Page<InspireComment> pg = commentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<InspireComment>()
                        .eq(InspireComment::getInspireId, inspireId)
                        .eq(InspireComment::getDeleted, 0)
                        .orderByDesc(InspireComment::getCreateTime));

        Page<CommentVO> voPage = new Page<>(pg.getCurrent(), pg.getSize(), pg.getTotal());
        voPage.setRecords(pg.getRecords().stream().map(c -> toVO(c)).toList());
        return voPage;
    }

    private CommentVO toVO(InspireComment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setInspireId(c.getInspireId());
        vo.setUserId(c.getUserId());
        vo.setUsername(c.getUsername());
        vo.setAvatar(c.getAvatar());
        try { vo.setNickname(jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, c.getUserId())); } catch(Exception e) { vo.setNickname(c.getUsername()); }        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setReplyUserId(c.getReplyUserId());
        vo.setReplyUsername(c.getReplyUsername());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    @Override
    @Transactional
    public void create(Long userId, CommentCreateRequest request) {
        InspireComment c = new InspireComment();
        c.setId(nextId());
        c.setInspireId(request.getInspireId());
        c.setUserId(userId);
        c.setUsername(request.getUsername() != null ? request.getUsername() : String.valueOf(userId));
        c.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        c.setContent(request.getContent());
        c.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        c.setReplyUserId(request.getReplyUserId() != null ? request.getReplyUserId() : 0L);
        c.setReplyUsername(request.getReplyUsername() != null ? request.getReplyUsername() : "");
        c.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        commentMapper.insert(c);
        log.info("评论创建: id={}, inspireId={}, userId={}, parentId={}",
                c.getId(), request.getInspireId(), userId, c.getParentId());

        // 通知灵感作者或被回复者
        try {
            String myName = jdbcTemplate.queryForObject(
                "SELECT nickname FROM user WHERE id=?", String.class, userId);
            if (myName == null) myName = String.valueOf(userId);
            String title = jdbcTemplate.queryForObject(
                "SELECT title FROM inspire_main WHERE id=?", String.class, request.getInspireId());
            if (title != null && title.length() > 20) title = title.substring(0, 20) + "...";

            if (request.getParentId() != null && request.getParentId() > 0L) {
                // 回复评论 → 通知被回复者
                String replyOwnerId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM inspire_comment WHERE id=?", String.class, request.getParentId());
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
    public void deleteById(Long commentId, Long userId) {
        commentMapper.delete(new LambdaQueryWrapper<InspireComment>()
                .eq(InspireComment::getId, commentId)
                .eq(InspireComment::getUserId, userId));
        commentMapper.delete(new LambdaQueryWrapper<InspireComment>()
                .eq(InspireComment::getParentId, commentId));
        log.info("评论删除: commentId={}, userId={}", commentId, userId);
    }

    private static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) ts = lastTs;
        if (ts == lastTs) { seq = (seq + 1) & 0xFFF; if (seq == 0) ts++; }
        else seq = 0;
        lastTs = ts;
        return ((ts - 1735689600000L) << 22) | (1L << 12) | 1L;
    }
}
