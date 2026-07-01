package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;
import com.inspire.platform.core.entity.InspireComment;
import com.inspire.platform.core.mapper.InspireCommentMapper;
import com.inspire.platform.core.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final InspireCommentMapper commentMapper;

    private static long seq = 0L, lastTs = -1L;

    @Override
    public Page<CommentVO> listByInspireId(Long inspireId, int page, int size) {
        // 查询所有评论（按时间倒序，顶级评论 + 回复都在同一批）
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
        vo.setContent(c.getContent());
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
        commentMapper.insert(c);
        log.info("评论创建: id={}, inspireId={}, userId={}, parentId={}",
                c.getId(), request.getInspireId(), userId, c.getParentId());
    }

    @Override
    @Transactional
    public void deleteById(Long commentId, Long userId) {
        // 删除评论及其所有子回复
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
