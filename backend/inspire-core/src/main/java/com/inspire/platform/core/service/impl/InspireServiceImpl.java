package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.*;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.InspireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspireServiceImpl implements InspireService {

    private final InspireMainMapper mainMapper;
    private final InspireContentMapper contentMapper;
    private final CollectMapper collectMapper;
    private final LikeMapper likeMapper;

    // ==================== 公开接口 ====================

    @Override
    public List<InspireVO> listPublic(InspirePageQuery query, Long loginUserId) {
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(InspireMain::getStatus, 1);
        wrapper.eq(InspireMain::getDeleted, 0);
        if (query.getTag() != null && !query.getTag().isEmpty())
            wrapper.eq(InspireMain::getTag, query.getTag());
        if ("heat".equals(query.getSort()))
            wrapper.orderByDesc(InspireMain::getHeat);
        else
            wrapper.orderByDesc(InspireMain::getCreateTime);

        int offset = (query.getPage() - 1) * query.getSize();
        wrapper.last("LIMIT " + query.getSize() + " OFFSET " + offset);
        List<InspireMain> list = mainMapper.selectList(wrapper);
        return list.stream().map(m -> toVO(m, loginUserId, null)).collect(Collectors.toList());
    }

    @Override
    public InspireVO getDetail(Long id, Long loginUserId) {
        InspireMain main = mainMapper.selectById(id);
        if (main == null || main.getDeleted() == 1 || main.getStatus() != 1)
            throw new BusinessException("灵感不存在");
        main.setViewCount(main.getViewCount() + 1);
        mainMapper.updateById(main);
        InspireContent content = contentMapper.selectById(id);
        return toVO(main, loginUserId, content != null ? content.getContent() : "");
    }

    // ==================== 我的 ====================

    @Override
    public List<InspireVO> listMyPublished(Long userId) {
        return queryByUser(userId, 1);
    }

    @Override
    public List<InspireVO> listMyDrafts(Long userId) {
        return queryByUser(userId, 0);
    }

    private List<InspireVO> queryByUser(Long userId, int status) {
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(InspireMain::getUserId, userId);
        wrapper.eq(InspireMain::getStatus, status);
        wrapper.eq(InspireMain::getDeleted, 0);
        wrapper.orderByDesc(InspireMain::getCreateTime);
        List<InspireMain> list = mainMapper.selectList(wrapper);
        return list.stream().map(m -> toVO(m, userId, null)).collect(Collectors.toList());
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional
    public InspireMain create(InspireCreateRequest req, Long userId) {
        InspireMain main = new InspireMain();
        main.setId(nextId());
        main.setTitle(req.getTitle());
        main.setImg(req.getImg() != null ? req.getImg() : "");
        main.setTag(req.getTag());
        main.setUserId(userId);
        main.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        main.setPublishCity(req.getPublishCity() != null ? req.getPublishCity() : "");
        main.setViewCount(0L); main.setLikeCount(0); main.setCollectCount(0); main.setHeat(0);
        mainMapper.insert(main);

        InspireContent content = new InspireContent();
        content.setInspireId(main.getId());
        content.setContent(req.getContent());
        contentMapper.insert(content);
        log.info("灵感创建: id={}, userId={}, status={}", main.getId(), userId, main.getStatus());
        return main;
    }

    @Override
    @Transactional
    public InspireMain update(Long id, InspireUpdateRequest req, Long userId) {
        InspireMain main = mainMapper.selectById(id);
        if (main == null || main.getDeleted() == 1)
            throw new BusinessException("灵感不存在");
        if (!main.getUserId().equals(userId))
            throw new BusinessException("只能修改自己的灵感");
        if (req.getTitle() != null) main.setTitle(req.getTitle());
        if (req.getTag() != null) main.setTag(req.getTag());
        if (req.getImg() != null) main.setImg(req.getImg());
        if (req.getStatus() != null) main.setStatus(req.getStatus());
        if (req.getPublishCity() != null) main.setPublishCity(req.getPublishCity());
        mainMapper.updateById(main);
        if (req.getContent() != null) {
            InspireContent content = contentMapper.selectById(id);
            if (content != null) { content.setContent(req.getContent()); contentMapper.updateById(content); }
            else { InspireContent nc = new InspireContent(); nc.setInspireId(id); nc.setContent(req.getContent()); contentMapper.insert(nc); }
        }
        return main;
    }

    @Override
    public void deleteById(Long id, Long userId) {
        InspireMain main = mainMapper.selectById(id);
        if (main == null || main.getDeleted() == 1) return;
        if (!main.getUserId().equals(userId))
            throw new BusinessException("只能删除自己的灵感");
        main.setDeleted(1);
        mainMapper.updateById(main);
    }

    // ==================== 收藏（分表 user_id % 10） ====================

    @Override
    @Transactional
    public void collect(Long userId, Long inspireId) {
        ShardContext.setByUserId(userId);
        try {
            LambdaQueryWrapper<CollectAction> w = Wrappers.lambdaQuery();
            w.eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId);
            if (collectMapper.selectOne(w) != null)
                throw new BusinessException("已收藏，请勿重复操作");
            CollectAction a = new CollectAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            collectMapper.insert(a);
        } finally { ShardContext.clear(); }
        // ShardContext已清除，此时操作inspire_main不会受影响
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = collect_count + 1").eq(InspireMain::getId, inspireId));
    }

    @Override
    @Transactional
    public void uncollect(Long userId, Long inspireId) {
        ShardContext.setByUserId(userId);
        try {
            LambdaQueryWrapper<CollectAction> w = Wrappers.lambdaQuery();
            w.eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId);
            collectMapper.delete(w);
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = GREATEST(collect_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }

    // ==================== 点赞（分表 inspire_id % 10） ====================

    @Override
    @Transactional
    public void like(Long userId, Long inspireId) {
        ShardContext.setByInspireId(inspireId);
        try {
            LambdaQueryWrapper<LikeAction> w = Wrappers.lambdaQuery();
            w.eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId);
            if (likeMapper.selectOne(w) != null)
                throw new BusinessException("已点赞，请勿重复操作");
            LikeAction a = new LikeAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            likeMapper.insert(a);
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("like_count = like_count + 1").eq(InspireMain::getId, inspireId));
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long inspireId) {
        ShardContext.setByInspireId(inspireId);
        try {
            LambdaQueryWrapper<LikeAction> w = Wrappers.lambdaQuery();
            w.eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId);
            likeMapper.delete(w);
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("like_count = GREATEST(like_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }

    // ==================== VO 组装 ====================

    private InspireVO toVO(InspireMain m, Long loginUserId, String content) {
        InspireVO vo = new InspireVO();
        vo.setId(m.getId()); vo.setTitle(m.getTitle()); vo.setImg(m.getImg());
        vo.setTag(m.getTag()); vo.setViewCount(m.getViewCount()); vo.setHeat(m.getHeat());
        vo.setLikeCount(m.getLikeCount()); vo.setCollectCount(m.getCollectCount());
        vo.setPublishCity(m.getPublishCity()); vo.setCreateTime(m.getCreateTime());
        vo.setContent(content);
        if (loginUserId != null) {
            ShardContext.setByUserId(loginUserId);
            try { vo.setCollected(collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, loginUserId).eq(CollectAction::getInspireId, m.getId())) != null);
            } finally { ShardContext.clear(); }
            ShardContext.setByInspireId(m.getId());
            try { vo.setLiked(likeMapper.selectOne(Wrappers.lambdaQuery(LikeAction.class)
                    .eq(LikeAction::getInspireId, m.getId()).eq(LikeAction::getUserId, loginUserId)) != null);
            } finally { ShardContext.clear(); }
        }
        return vo;
    }

    private static long seq = 0L;
    private static long lastTs = -1L;
    private static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) ts = lastTs;
        if (ts == lastTs) { seq = (seq + 1) & 0xFFF; if (seq == 0) ts++; }
        else seq = 0;
        lastTs = ts;
        return ((ts - 1735689600000L) << 22) | (1L << 12) | 1L;
    }
}
