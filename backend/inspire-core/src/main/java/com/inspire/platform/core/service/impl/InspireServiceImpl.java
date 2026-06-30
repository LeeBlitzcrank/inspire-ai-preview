package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.*;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.InspireService;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import com.inspire.platform.core.service.es.EsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EsSyncService esSyncService;
    private final MqProducer mqProducer;

    @Override
    public List<InspireVO> listPublic(InspirePageQuery query, Long loginUserId) {
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(InspireMain::getStatus, 1).eq(InspireMain::getDeleted, 0);
        if (query.getTag() != null && !query.getTag().isEmpty()) wrapper.eq(InspireMain::getTag, query.getTag());
        if ("heat".equals(query.getSort())) wrapper.orderByDesc(InspireMain::getHeat);
        else wrapper.orderByDesc(InspireMain::getCreateTime);
        int off = (query.getPage() - 1) * query.getSize();
        wrapper.last("LIMIT " + query.getSize() + " OFFSET " + off);
        return mainMapper.selectList(wrapper).stream().map(m -> toVO(m, loginUserId, null)).collect(Collectors.toList());
    }

    @Override
    public InspireVO getDetail(Long id, Long loginUserId) {
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1 || m.getStatus() != 1) throw new BusinessException("灵感不存在");
        m.setViewCount(m.getViewCount() + 1); mainMapper.updateById(m);
        InspireContent c = contentMapper.selectById(id);
        return toVO(m, loginUserId, c != null ? c.getContent() : "");
    }

    @Override
    public List<InspireVO> listMyPublished(Long userId) { return queryByUser(userId, 1); }
    @Override public List<InspireVO> listMyDrafts(Long userId) { return queryByUser(userId, 0); }

    private List<InspireVO> queryByUser(Long userId, int status) {
        LambdaQueryWrapper<InspireMain> w = Wrappers.lambdaQuery();
        w.eq(InspireMain::getUserId, userId).eq(InspireMain::getStatus, status).eq(InspireMain::getDeleted, 0)
         .orderByDesc(InspireMain::getCreateTime);
        return mainMapper.selectList(w).stream().map(m -> toVO(m, userId, null)).collect(Collectors.toList());
    }

    @Override @Transactional
    public InspireMain create(InspireCreateRequest req, Long userId) {
        InspireMain m = new InspireMain();
        m.setId(nextId()); m.setTitle(req.getTitle());
        m.setImg(req.getImg() != null ? req.getImg() : ""); m.setTag(req.getTag()); m.setUserId(userId);
        m.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        m.setPublishCity(req.getPublishCity() != null ? req.getPublishCity() : "");
        m.setViewCount(0L); m.setLikeCount(0); m.setCollectCount(0); m.setHeat(0);
        mainMapper.insert(m);
        InspireContent c = new InspireContent(); c.setInspireId(m.getId()); c.setContent(req.getContent());
        contentMapper.insert(c);
        mqProducer.send(MqTopicConstants.TOPIC_INSPIRE_PUBLISH, java.util.Map.of("inspireId", m.getId(), "userId", userId, "title", m.getTitle(), "tag", m.getTag()));
        esSyncService.sync(m);
        log.info("创建灵感: id={}, userId={}, status={}", m.getId(), userId, m.getStatus());
        return m;
    }

    @Override @Transactional
    public InspireMain update(Long id, InspireUpdateRequest req, Long userId) {
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) throw new BusinessException("灵感不存在");
        if (!m.getUserId().equals(userId)) throw new BusinessException("只能修改自己的灵感");
        if (req.getTitle() != null) m.setTitle(req.getTitle());
        if (req.getTag() != null) m.setTag(req.getTag());
        if (req.getImg() != null) m.setImg(req.getImg());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        if (req.getPublishCity() != null) m.setPublishCity(req.getPublishCity());
        mainMapper.updateById(m);
        if (req.getContent() != null) {
            InspireContent c = contentMapper.selectById(id);
            if (c != null) { c.setContent(req.getContent()); contentMapper.updateById(c); }
            else { InspireContent nc = new InspireContent(); nc.setInspireId(id); nc.setContent(req.getContent()); contentMapper.insert(nc); }
        }
        esSyncService.sync(m);
        return m;
    }

    @Override
    public void deleteById(Long id, Long userId) {
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) return;
        if (!m.getUserId().equals(userId)) throw new BusinessException("只能删除自己的灵感");
        m.setDeleted(1); mainMapper.updateById(m);
        esSyncService.delete(m.getId());
    }

    @Override @Transactional
    public void collect(Long userId, Long inspireId) {
        ShardContext.setByUserId(userId);
        try {
            if (collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId)) != null)
                throw new BusinessException("已收藏");
            CollectAction a = new CollectAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            collectMapper.insert(a);
        } finally { ShardContext.clear(); }
        InspireMain inspireForMsg = mainMapper.selectById(inspireId);
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = collect_count + 1").eq(InspireMain::getId, inspireId));
        mqProducer.send(MqTopicConstants.TOPIC_USER_BEHAVIOR, java.util.Map.of(
                "userId", userId, "inspireId", inspireId, "type", "collect",
                "tag", inspireForMsg != null ? inspireForMsg.getTag() : "",
                "city", inspireForMsg != null ? inspireForMsg.getPublishCity() : ""));
    }

    @Override @Transactional
    public void uncollect(Long userId, Long inspireId) {
        ShardContext.setByUserId(userId);
        try { collectMapper.delete(Wrappers.lambdaQuery(CollectAction.class)
                .eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId));
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = GREATEST(collect_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }

    @Override @Transactional
    public void like(Long userId, Long inspireId) {
        ShardContext.setByInspireId(inspireId);
        try {
            if (likeMapper.selectOne(Wrappers.lambdaQuery(LikeAction.class)
                    .eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId)) != null)
                throw new BusinessException("已点赞");
            LikeAction a = new LikeAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            likeMapper.insert(a);
        } finally { ShardContext.clear(); }
        InspireMain inspireForMsg = mainMapper.selectById(inspireId);
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("like_count = like_count + 1").eq(InspireMain::getId, inspireId));
        mqProducer.send(MqTopicConstants.TOPIC_USER_BEHAVIOR, java.util.Map.of(
                "userId", userId, "inspireId", inspireId, "type", "like",
                "tag", inspireForMsg != null ? inspireForMsg.getTag() : "",
                "city", inspireForMsg != null ? inspireForMsg.getPublishCity() : ""));
    }

    @Override @Transactional
    public void unlike(Long userId, Long inspireId) {
        ShardContext.setByInspireId(inspireId);
        try { likeMapper.delete(Wrappers.lambdaQuery(LikeAction.class)
                .eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId));
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("like_count = GREATEST(like_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }

    @Override
    public List<InspireVO> listMyCollects(Long userId) {
        List<Long> ids;
        ShardContext.setByUserId(userId);
        try {
            List<CollectAction> collects = collectMapper.selectList(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId).orderByDesc(CollectAction::getCreateTime));
            if (collects.isEmpty()) return new ArrayList<>();
            ids = collects.stream().map(CollectAction::getInspireId).collect(Collectors.toList());
        } finally { ShardContext.clear(); }
        List<InspireMain> mains = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .in(InspireMain::getId, ids).eq(InspireMain::getDeleted, 0));
        return mains.stream().map(m -> toVO(m, userId, null)).collect(Collectors.toList());
    }

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
