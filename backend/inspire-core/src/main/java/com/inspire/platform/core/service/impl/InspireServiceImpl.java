package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.*;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.InspireService;
import com.inspire.platform.common.util.TextFilter;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import com.inspire.platform.core.service.es.EsSyncService;
import com.inspire.platform.core.service.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspireServiceImpl implements InspireService {

    private final InspireMainMapper mainMapper;
    private final InspireContentMapper contentMapper;
    private final CollectMapper collectMapper;
    private final CollectFolderMapper collectFolderMapper;
    private final LikeMapper likeMapper;
    private final EsSyncService esSyncService;
    private final NotificationService notificationService;
    private final MqProducer mqProducer;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "publicList", key = "#query.page + ':' + #query.size + ':' + #query.tag + ':' + #query.sort", unless = "#loginUserId != null")
    public List<InspireVO> listPublic(InspirePageQuery query, Long loginUserId) {
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(InspireMain::getStatus, 1).eq(InspireMain::getDeleted, 0);
        if (query.getTag() != null && !query.getTag().isEmpty()) wrapper.eq(InspireMain::getTag, query.getTag());
        if ("heat".equals(query.getSort())) wrapper.orderByDesc(InspireMain::getHeat);
        else wrapper.orderByDesc(InspireMain::getCreateTime);
        Page<InspireMain> mpPage = mainMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return toVOList(mpPage.getRecords(), loginUserId);
    }

    @Override
    public InspireVO getDetail(Long id, Long loginUserId) {
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) throw new BusinessException("灵感不存在");
        if (m.getStatus() != 1 && (loginUserId == null || !m.getUserId().equals(loginUserId))) throw new BusinessException("灵感不存在");
        m.setViewCount(m.getViewCount() + 1); mainMapper.updateById(m);
        InspireContent c = contentMapper.selectById(id);
        return toVO(m, loginUserId, c != null ? c.getContent() : "");
    }

    @Override
    public PageResult<InspireVO> listMyPublished(Long userId, int page, int size) {
        return queryByUser(userId, 1, page, size);
    }

    @Override
    public PageResult<InspireVO> listMyDrafts(Long userId, int page, int size) {
        return queryByUser(userId, 0, page, size);
    }

    private PageResult<InspireVO> queryByUser(Long userId, int status, int page, int size) {
        LambdaQueryWrapper<InspireMain> w = Wrappers.lambdaQuery();
        w.eq(InspireMain::getUserId, userId).eq(InspireMain::getStatus, status).eq(InspireMain::getDeleted, 0)
         .orderByDesc(InspireMain::getCreateTime);
        Page<InspireMain> mpPage = mainMapper.selectPage(new Page<>(page, size), w);
        List<InspireVO> list = toVOList(mpPage.getRecords(), userId);
        log.info("[PAGEDBG] queryByUser userId={} status={} page={} size={} records={} total={}", userId, status, page, size, list.size(), mpPage.getTotal());
        return new PageResult<>(list, mpPage.getTotal());
    }

    @Override @Transactional
    public InspireMain create(InspireCreateRequest req, Long userId) {
        checkUserExists(userId);
        InspireMain m = new InspireMain();
        m.setId(nextId()); m.setTitle(req.getTitle());
        m.setImg(req.getImg() != null ? req.getImg() : ""); m.setImages(req.getImages() != null ? req.getImages() : ""); m.setTag(req.getTag()); m.setUserId(userId);
        // 内容审核：命中敏感词设为待审核（2），否则用请求的status
        String reason = TextFilter.check(req.getTitle());
        if (reason == null) reason = TextFilter.check(req.getContent());
        if (reason != null) {
            log.warn("内容触发审核: title={}, userId={}, reason={}", req.getTitle(), userId, reason);
            m.setStatus(2);
        } else {
            m.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        }
        m.setPublishCity(req.getPublishCity() != null ? req.getPublishCity() : "");
        m.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
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
        checkUserExists(userId);
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) throw new BusinessException("灵感不存在");
        if (!m.getUserId().equals(userId)) throw new BusinessException("只能修改自己的灵感");
        if (req.getTitle() != null) m.setTitle(req.getTitle());
        if (req.getTag() != null) m.setTag(req.getTag());
        if (req.getImg() != null) m.setImg(req.getImg());
        if (req.getImages() != null) m.setImages(req.getImages());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        if (req.getPublishCity() != null) m.setPublishCity(req.getPublishCity());
        
        // 保存当前版本到历史（仅已发布的灵感）
        if (m.getStatus() == 1) {
            try {
                Integer verNum = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(version_number), 0) + 1 FROM inspire_version WHERE inspire_id = ?",
                    Integer.class, id);
                InspireContent oldContent = contentMapper.selectById(id);
                jdbcTemplate.update(
                    "INSERT INTO inspire_version(id, inspire_id, version_number, title, content, img, images, tag) VALUES(?,?,?,?,?,?,?,?)",
                    com.inspire.platform.core.service.impl.InspireServiceImpl.nextId(), id, verNum,
                    m.getTitle() != null ? m.getTitle() : "",
                    oldContent != null ? oldContent.getContent() : "",
                    m.getImg() != null ? m.getImg() : "",
                    m.getImages() != null ? m.getImages() : "",
                    m.getTag() != null ? m.getTag() : ""
                );
                log.info("Version saved: inspireId={}, version={}", id, verNum);
            } catch (Exception e) { log.warn("Version save failed", e); }
        }
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
        checkUserExists(userId);
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) return;
        if (!m.getUserId().equals(userId)) throw new BusinessException("只能删除自己的灵感");
        m.setDeleted(1); mainMapper.updateById(m);
        esSyncService.delete(m.getId());
    }

    @Override @Transactional
    public void collect(Long userId, Long inspireId) {
        checkUserExists(userId);
        collectToFolder(userId, inspireId, null);
    }

    @Override @Transactional
    public void collectToFolder(Long userId, Long inspireId, Long folderId) {
        checkUserExists(userId);
        ShardContext.setByUserId(userId);
        try {
            if (collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId)) != null)
                throw new BusinessException("已收藏");
            CollectAction a = new CollectAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            if (folderId != null) a.setFolderId(folderId);
            collectMapper.insert(a);
        } finally { ShardContext.clear(); }
        InspireMain inspireForMsg = mainMapper.selectById(inspireId);
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = collect_count + 1").eq(InspireMain::getId, inspireId));
        mqProducer.send(MqTopicConstants.TOPIC_USER_BEHAVIOR, java.util.Map.of(
                "userId", userId, "inspireId", inspireId, "type", "collect",
                "tag", inspireForMsg != null ? inspireForMsg.getTag() : "",
                "city", inspireForMsg != null ? inspireForMsg.getPublishCity() : ""));
        // 通知灵感作者
        try {
            String myName = jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, userId);
            if (myName == null) myName = String.valueOf(userId);
            String title = inspireForMsg != null ? inspireForMsg.getTitle() : "";
            if (title != null && title.length() > 20) title = title.substring(0, 20) + "...";
            if (inspireForMsg != null) {
                notificationService.notify(inspireForMsg.getUserId(), "collect", userId,
                    myName, "收藏了你的灵感", inspireId, title);
            }
        } catch (Exception e) { log.warn("收藏通知发送失败", e); }
    }

    @Override @Transactional
    public void uncollect(Long userId, Long inspireId) {
        checkUserExists(userId);
        ShardContext.setByUserId(userId);
        try { collectMapper.delete(Wrappers.lambdaQuery(CollectAction.class)
                .eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId));
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("collect_count = GREATEST(collect_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }

    @Override @Transactional
    public void like(Long userId, Long inspireId) {
        checkUserExists(userId);
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
        // 通知灵感作者
        try {
            String myName = jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, userId);
            if (myName == null) myName = String.valueOf(userId);
            String title = inspireForMsg != null ? inspireForMsg.getTitle() : "";
            if (title != null && title.length() > 20) title = title.substring(0, 20) + "...";
            if (inspireForMsg != null) {
                notificationService.notify(inspireForMsg.getUserId(), "like", userId,
                    myName, "点赞了你的灵感", inspireId, title);
            }
        } catch (Exception e) { log.warn("点赞通知发送失败", e); }
    }

    @Override @Transactional
    public void unlike(Long userId, Long inspireId) {
        checkUserExists(userId);

        ShardContext.setByInspireId(inspireId);
        try { likeMapper.delete(Wrappers.lambdaQuery(LikeAction.class)
                .eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId));
        } finally { ShardContext.clear(); }
        mainMapper.update(null, Wrappers.lambdaUpdate(InspireMain.class)
                .setSql("like_count = GREATEST(like_count - 1, 0)").eq(InspireMain::getId, inspireId));
    }


    @Override
    @Transactional
    public void share(Long userId, Long inspireId) {
        checkUserExists(userId);
        mainMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate(InspireMain.class)
            .setSql("share_count = COALESCE(share_count, 0) + 1").eq(InspireMain::getId, inspireId));
        mqProducer.send(com.inspire.platform.mq.constant.MqTopicConstants.TOPIC_USER_BEHAVIOR, java.util.Map.of(
            "userId", userId, "inspireId", inspireId, "type", "share"));
        log.info("分享: userId={}, inspireId={}", userId, inspireId);
    }

    @Override
    public PageResult<InspireVO> listMyCollects(Long userId, int page, int size) {
        List<Long> ids;
        long total = 0;
        ShardContext.setByUserId(userId);
        try {
            List<CollectAction> collects = collectMapper.selectList(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId).orderByDesc(CollectAction::getCreateTime));
            total = collects.size();
            log.info("[PAGEDBG] listMyCollects userId={} page={} size={} totalCollects={}", userId, page, size, total);
            int start = (page - 1) * size;
            if (start >= collects.size()) {
                log.info("[PAGEDBG] start={} >= collects.size={} -> empty", start, collects.size());
                return new PageResult<>(new ArrayList<>(), total);
            }
            int end = Math.min(start + size, collects.size());
            log.info("[PAGEDBG] start={} end={} records={}", start, end, end - start);
            collects = collects.subList(start, end);
            if (collects.isEmpty()) return new PageResult<>(new ArrayList<>(), total);
            ids = collects.stream().map(CollectAction::getInspireId).collect(Collectors.toList());
        } finally { ShardContext.clear(); }
        List<InspireMain> mains = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .in(InspireMain::getId, ids).eq(InspireMain::getDeleted, 0));
        log.info("[PAGEDBG] found mains={} total={}", mains.size(), total);
        return new PageResult<>(toVOList(mains, userId), total);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "recommend", key = "#page + ':' + #size", unless = "#userId != null")
    public List<InspireVO> recommend(Long userId, int page, int size) {
        LambdaQueryWrapper<InspireMain> w = Wrappers.lambdaQuery();
        w.eq(InspireMain::getStatus, 1).eq(InspireMain::getDeleted, 0);
        w.orderByDesc(InspireMain::getHeat, InspireMain::getCreateTime);
        Page<InspireMain> mpPage = mainMapper.selectPage(new Page<>(page, size), w);
        return toVOList(mpPage.getRecords(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> listVersions(Long inspireId) {
        return jdbcTemplate.query(
            "SELECT id, version_number, title, tag, change_summary, create_time FROM inspire_version WHERE inspire_id = ? ORDER BY version_number DESC",
            (rs, n) -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", String.valueOf(rs.getLong("id")));
                m.put("versionNumber", rs.getInt("version_number"));
                m.put("title", rs.getString("title"));
                m.put("tag", rs.getString("tag"));
                m.put("changeSummary", rs.getString("change_summary"));
                m.put("createTime", rs.getTimestamp("create_time") != null ?
                    rs.getTimestamp("create_time").toLocalDateTime().toString() : "");
                return m;
            }, inspireId);
    }

    @Override
    public Map<String, Object> getVersion(Long versionId) {
        try {
            Map<String, Object> m = jdbcTemplate.queryForMap(
                "SELECT id, inspire_id, version_number, title, content, img, images, tag, create_time FROM inspire_version WHERE id = ?",
                versionId);
            Map<String, Object> result = new HashMap<>();
            result.put("id", String.valueOf(m.get("id")));
            result.put("inspireId", String.valueOf(m.get("inspire_id")));
            result.put("versionNumber", m.get("version_number"));
            result.put("title", m.get("title"));
            result.put("content", m.get("content"));
            result.put("img", m.get("img") != null ? m.get("img") : "");
            result.put("images", m.get("images") != null ? m.get("images") : "");
            result.put("tag", m.get("tag"));
            result.put("createTime", m.get("create_time") != null ? m.get("create_time").toString() : "");
            return result;
        } catch (Exception e) {
            log.warn("版本详情查询失败: versionId={}", versionId);
            return null;
        }
    }

    /** 公开的静态ID生成器 */
    public static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) ts = lastTs;
        if (ts == lastTs) { seq = (seq + 1) & 0xFFF; if (seq == 0) ts++; }
        else seq = 0;
        lastTs = ts;
        return ((ts - 1735689600000L) << 22) | (1L << 12) | 1L;
    }

    @Override
    public List<CollectFolder> getCollectFolders(Long userId) {
        return collectFolderMapper.selectList(Wrappers.lambdaQuery(CollectFolder.class)
                .eq(CollectFolder::getUserId, userId).orderByAsc(CollectFolder::getSortOrder));
    }

    @Override @Transactional
    public CollectFolder createCollectFolder(Long userId, String name, String icon) {
        checkUserExists(userId);
        CollectFolder f = new CollectFolder();
        f.setId(nextId()); f.setUserId(userId); f.setName(name);
        f.setIcon(icon != null ? icon : "📁"); f.setSortOrder(0);
        collectFolderMapper.insert(f);
        return f;
    }

    @Override @Transactional
    public void deleteCollectFolder(Long userId, Long folderId) {
        checkUserExists(userId);
        CollectFolder f = collectFolderMapper.selectById(folderId);
        if (f == null || !f.getUserId().equals(userId)) throw new BusinessException("文件夹不存在");
        collectFolderMapper.deleteById(folderId);
        // 将该文件夹下的收藏记录 folder_id 置空
        for (int i = 0; i < 10; i++) {
            try {
                jdbcTemplate.update("UPDATE collect_" + i + " SET folder_id = NULL WHERE folder_id = ?", folderId);
            } catch (Exception ignored) {}
        }
    }

    @Override @Transactional
    public void renameCollectFolder(Long userId, Long folderId, String name) {
        checkUserExists(userId);
        CollectFolder f = collectFolderMapper.selectById(folderId);
        if (f == null || !f.getUserId().equals(userId)) throw new BusinessException("文件夹不存在");
        f.setName(name);
        collectFolderMapper.updateById(f);
    }

    @Override
    public List<InspireVO> listCollectsByFolder(Long userId, Long folderId) {
        ShardContext.setByUserId(userId);
        try {
            List<CollectAction> collects;
            if (folderId != null && folderId > 0) {
                collects = collectMapper.selectList(Wrappers.lambdaQuery(CollectAction.class)
                        .eq(CollectAction::getUserId, userId)
                        .eq(CollectAction::getFolderId, folderId)
                        .orderByDesc(CollectAction::getCreateTime));
            } else {
                collects = collectMapper.selectList(Wrappers.lambdaQuery(CollectAction.class)
                        .eq(CollectAction::getUserId, userId)
                        .isNull(CollectAction::getFolderId)
                        .orderByDesc(CollectAction::getCreateTime));
            }
            if (collects.isEmpty()) return List.of();
            List<Long> ids = collects.stream().map(CollectAction::getInspireId).collect(Collectors.toList());
            // 查 inspire_main 前清除分片（inspire_main 不分表）
            ShardContext.clear();
            List<InspireMain> mains = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                    .in(InspireMain::getId, ids));
            Map<Long, InspireMain> map = mains.stream().collect(Collectors.toMap(InspireMain::getId, m -> m));
            List<InspireVO> result = new ArrayList<>();
            // Build nickname map + collect/like status in batch
            Map<Long, String> nicknameMap = buildNicknameMap(mains);
            Set<Long> collectedIds = buildCollectedIds(userId, ids);
            Set<Long> likedIds = buildLikedIds(ids, userId);
            for (CollectAction c : collects) {
                InspireMain m = map.get(c.getInspireId());
                if (m != null) {
                    InspireVO vo = singleToVO(m, nicknameMap.get(m.getUserId()), null);
                    vo.setCollected(true);
                    vo.setLiked(likedIds.contains(m.getId()));
                    result.add(vo);
                }
            }
            return result;
        } finally { ShardContext.clear(); }
    }


    @Override @Transactional
    public void moveCollectToFolder(Long userId, Long inspireId, Long folderId) {
        checkUserExists(userId);
        ShardContext.setByUserId(userId);
        try {
            CollectAction a = collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId)
                    .eq(CollectAction::getInspireId, inspireId));
            if (a != null) {
                a.setFolderId(folderId);
                collectMapper.updateById(a);
            }
        } finally { ShardContext.clear(); }
    }


    @Override
    public java.util.List<java.util.Map<String, Object>> getHotTags() {
        return jdbcTemplate.queryForList(
            "SELECT tag, COUNT(*) as count FROM inspire_main WHERE deleted=0 AND status=1 GROUP BY tag ORDER BY count DESC LIMIT 20");
    }

    // ========================
    // 批量转换：解决 N+1 查询
    // ========================

    /**
     * 批量将 InspireMain 列表转为 InspireVO，将 N+1 次 nickname/collect/like 查询
     * 合并为至多 12 次批量查询（nickname × 1 + collect × 1 + like × ≤10）。
     */
    private List<InspireVO> toVOList(List<InspireMain> mains, Long loginUserId) {
        if (mains.isEmpty()) return Collections.emptyList();

        // 1. Batch query nicknames
        Map<Long, String> nicknameMap = buildNicknameMap(mains);

        // 2. Batch query collect/like status
        Set<Long> collectedIds = new HashSet<>();
        Set<Long> likedIds = new HashSet<>();
        if (loginUserId != null) {
            List<Long> inspireIds = mains.stream().map(InspireMain::getId).collect(Collectors.toList());
            collectedIds = buildCollectedIds(loginUserId, inspireIds);
            likedIds = buildLikedIds(inspireIds, loginUserId);
        }

        // 3. Build VOs
        List<InspireVO> result = new ArrayList<>(mains.size());
        for (InspireMain m : mains) {
            InspireVO vo = singleToVO(m, nicknameMap.get(m.getUserId()), null);
            if (loginUserId != null) {
                vo.setCollected(collectedIds.contains(m.getId()));
                vo.setLiked(likedIds.contains(m.getId()));
            }
            result.add(vo);
        }
        return result;
    }

    /** 批量查 nickname */
    private Map<Long, String> buildNicknameMap(List<InspireMain> mains) {
        Map<Long, String> map = new HashMap<>();
        Set<Long> userIds = mains.stream().map(InspireMain::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) return map;
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        try {
            jdbcTemplate.query("SELECT id, nickname FROM user WHERE id IN (" + placeholders + ")",
                (rs) -> { map.put(rs.getLong("id"), rs.getString("nickname")); },
                userIds.toArray());
        } catch (Exception e) {
            log.warn("批量查询昵称失败", e);
        }
        return map;
    }

    /** 批量查当前用户是否收藏了这些灵感（单分片查询，1次） */
    private Set<Long> buildCollectedIds(Long userId, List<Long> inspireIds) {        if (inspireIds.isEmpty()) return Collections.emptySet();        int shard = (int)(Math.abs(userId) % 10);        String placeholders = inspireIds.stream().map(id -> "?").collect(Collectors.joining(","));        try {            List<Object> params = new ArrayList<>();            params.add(userId);            params.addAll(inspireIds);            List<Long> found = jdbcTemplate.queryForList(                "SELECT inspire_id FROM collect_" + shard + " WHERE user_id = ? AND inspire_id IN (" + placeholders + ")",                Long.class, params.toArray());            return new HashSet<>(found);        } catch (Exception e) {            log.warn("批量查询收藏状态失败", e);            return Collections.emptySet();        }    }    private Set<Long> buildLikedIds(List<Long> inspireIds, Long userId) {        if (inspireIds.isEmpty()) return Collections.emptySet();        Set<Long> liked = new HashSet<>();        Map<Integer, List<Long>> grouped = inspireIds.stream()                .collect(Collectors.groupingBy(id -> (int)(Math.abs(id) % 10)));        for (Map.Entry<Integer, List<Long>> entry : grouped.entrySet()) {            int shard = entry.getKey();            List<Long> ids = entry.getValue();            String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));            try {                List<Object> params = new ArrayList<>();                params.add(userId);                params.addAll(ids);                List<Long> found = jdbcTemplate.queryForList(                    "SELECT inspire_id FROM inspire_like_" + shard + " WHERE user_id = ? AND inspire_id IN (" + placeholders + ")",                    Long.class, params.toArray());                liked.addAll(found);            } catch (Exception e) {                log.warn("批量查询点赞状态失败: shard={}", shard, e);            }        }        return liked;    }    private InspireVO toVO(InspireMain m, Long loginUserId, String content) {
        String nickname = "";
        try { nickname = jdbcTemplate.queryForObject("SELECT nickname FROM user WHERE id=?", String.class, m.getUserId()); } catch(Exception e) {}
        InspireVO vo = singleToVO(m, nickname, content);
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

    /** 基础字段填充（不含 collect/like 状态） */
    private InspireVO singleToVO(InspireMain m, String nickname, String content) {
        InspireVO vo = new InspireVO();
        vo.setUserId(m.getUserId()); vo.setId(m.getId());
        vo.setNickname(nickname != null ? nickname : "");
        vo.setTitle(m.getTitle()); vo.setImg(m.getImg());
        if (m.getImages() != null && !m.getImages().isEmpty()) {
            try { vo.setImages(objectMapper.readValue(m.getImages(), new TypeReference<java.util.List<String>>() {})); }
            catch (Exception e) { log.warn("解析多图失败", e); }
        }
        vo.setTag(m.getTag()); vo.setViewCount(m.getViewCount()); vo.setHeat(m.getHeat());
        vo.setShareCount(m.getShareCount());
        vo.setLikeCount(m.getLikeCount()); vo.setCollectCount(m.getCollectCount());
        vo.setPublishCity(m.getPublishCity()); vo.setCreateTime(m.getCreateTime());
        vo.setContent(content);
        return vo;
    }

    private static long seq = 0L, lastTs = -1L;


    /** 校验用户是否存在（未注册/已注销则抛出 403） */
    private void checkUserExists(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `user` WHERE id = ? AND deleted = 0",
            Integer.class, userId);
        if (count == null || count == 0) {
            throw new BusinessException(403, "用户不存在或已被注销");
        }
    }


    private String loadAvatar(Long userId) {
        try { return jdbcTemplate.queryForObject("SELECT avatar FROM user WHERE id=?", String.class, userId); }
        catch (Exception e) { return ""; }
    }

}
