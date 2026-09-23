package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.util.TextFilter;
import com.inspire.platform.common.util.TitleUtil;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.*;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.InspireService;
import com.inspire.platform.core.service.NotificationService;
import com.inspire.platform.core.service.es.EsSyncService;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspireServiceImpl implements InspireService {

    private final InspireMainMapper mainMapper;
    private final InspireContentMapper contentMapper;
    private final CollectMapper collectMapper;
    private final CollectFolderMapper collectFolderMapper;
    private final InspireSeriesMapper seriesMapper;
    private final LikeMapper likeMapper;
    private final EsSyncService esSyncService;
    private final NotificationService notificationService;
    private final MqProducer mqProducer;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${inspire.series.max-per-user:50}")
    private int maxSeriesPerUser;

    @Value("${inspire.series.max-articles:100}")
    private int maxArticlesPerSeries;

    private record UserRelationState(Set<Long> collectedIds, Set<Long> likedIds) {
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "publicList", key = "#query.page + ':' + #query.size + ':' + #query.tag + ':' + #query.sort", unless = "#loginUserId != null")
    public List<InspireVO> listPublic(InspirePageQuery query, Long loginUserId) {
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(InspireMain::getStatus, 1).eq(InspireMain::getDeleted, 0);
        if (query.getTag() != null && !query.getTag().isEmpty()) {
            wrapper.eq(InspireMain::getTag, query.getTag());
        }
        if ("heat".equals(query.getSort())) {
            wrapper.orderByDesc(InspireMain::getHeat);
        } else {
            wrapper.orderByDesc(InspireMain::getCreateTime);
        }
        Page<InspireMain> mpPage = mainMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return toVOList(mpPage.getRecords(), loginUserId);
    }

    @Override
    public InspireVO getDetail(Long id, Long loginUserId) {
        InspireMain m = mainMapper.selectById(id);
        if (m == null || m.getDeleted() == 1) {
            throw new BusinessException("灵感不存在");
        }
        if (m.getStatus() != 1 && (loginUserId == null || !m.getUserId().equals(loginUserId))) {
            throw new BusinessException("灵感不存在");
        }
        // 原子更新单列，避免详情页把整行字段重新写一遍，也避免并发下的计数丢失。
        jdbcTemplate.update("UPDATE inspire_main SET view_count = view_count + 1 WHERE id = ?", id);
        m.setViewCount(m.getViewCount() + 1);
        InspireContent c = contentMapper.selectById(id);
        InspireVO vo = toDetailVO(m, loginUserId, c != null ? c.getContent() : "");
        fillSeriesContext(vo, m);
        return vo;
    }

    @Override
    public SeriesVO getSeries(Long id, Long loginUserId) {
        InspireSeries series = seriesMapper.selectById(id);
        if (series == null || Integer.valueOf(1).equals(series.getDeleted())
                || !Integer.valueOf(1).equals(series.getStatus())) {
            throw new BusinessException("系列不存在");
        }
        return buildSeriesVO(series, loadSeriesArticles(id), loginUserId);
    }

    @Override
    public List<SeriesVO> listMySeries(Long userId) {
        checkUserExists(userId);
        List<InspireSeries> seriesList = seriesMapper.selectList(Wrappers.lambdaQuery(InspireSeries.class)
                .eq(InspireSeries::getUserId, userId)
                .eq(InspireSeries::getStatus, 1)
                .eq(InspireSeries::getDeleted, 0)
                .orderByDesc(InspireSeries::getUpdateTime)
                .orderByDesc(InspireSeries::getId));
        if (seriesList.isEmpty()) return Collections.emptyList();

        List<Long> seriesIds = seriesList.stream().map(InspireSeries::getId).collect(Collectors.toList());
        String placeholders = seriesIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(seriesIds);
        Map<Long, Integer> counts = new HashMap<>();
        List<Map<String, Object>> countRows = jdbcTemplate.queryForList(
                "SELECT series_id, COUNT(*) AS total FROM inspire_main "
                        + "WHERE user_id = ? AND deleted = 0 AND status = 1 AND series_id IN (" + placeholders + ") "
                        + "GROUP BY series_id",
                params.toArray());
        for (Map<String, Object> row : countRows) {
            counts.put(((Number) row.get("series_id")).longValue(),
                    ((Number) row.get("total")).intValue());
        }

        List<SeriesVO> result = new ArrayList<>(seriesList.size());
        for (InspireSeries series : seriesList) {
            SeriesVO vo = buildSeriesVO(series, Collections.emptyList(), userId);
            vo.setTotal(counts.getOrDefault(series.getId(), 0));
            result.add(vo);
        }
        return result;
    }

    @Override
    public SeriesVO getMySeries(Long userId, Long seriesId) {
        InspireSeries series = requireOwnedSeries(userId, seriesId);
        return buildSeriesVO(series, loadSeriesArticles(seriesId), userId);
    }

    @Override
    @Transactional
    public SeriesVO createSeries(Long userId, SeriesSaveRequest request) {
        checkUserExists(userId);
        Long count = seriesMapper.selectCount(Wrappers.lambdaQuery(InspireSeries.class)
                .eq(InspireSeries::getUserId, userId)
                .eq(InspireSeries::getDeleted, 0));
        if (count != null && count >= maxSeriesPerUser) {
            throw new BusinessException("最多只能创建 " + maxSeriesPerUser + " 个系列");
        }
        InspireSeries series = new InspireSeries();
        series.setId(nextId());
        series.setUserId(userId);
        series.setName(normalizeSeriesName(request.getName()));
        series.setDescription(normalizeSeriesDescription(request.getDescription()));
        series.setCover("");
        series.setStatus(1);
        series.setCreateTime(LocalDateTime.now());
        series.setUpdateTime(LocalDateTime.now());
        series.setDeleted(0);
        seriesMapper.insert(series);
        return buildSeriesVO(series, Collections.emptyList(), userId);
    }

    @Override
    @Transactional
    public SeriesVO updateSeries(Long userId, Long seriesId, SeriesSaveRequest request) {
        InspireSeries series = requireOwnedSeries(userId, seriesId);
        series.setName(normalizeSeriesName(request.getName()));
        series.setDescription(normalizeSeriesDescription(request.getDescription()));
        series.setUpdateTime(LocalDateTime.now());
        seriesMapper.updateById(series);
        return buildSeriesVO(series, loadSeriesArticles(seriesId), userId);
    }

    @Override
    @Transactional
    public void deleteSeries(Long userId, Long seriesId) {
        requireOwnedSeries(userId, seriesId);
        jdbcTemplate.update(
                "UPDATE inspire_main SET series_id = NULL, series_order = 0 WHERE series_id = ? AND user_id = ?",
                seriesId, userId);
        seriesMapper.deleteById(seriesId);
    }

    @Override
    @Transactional
    public SeriesVO addSeriesArticle(Long userId, Long seriesId, Long inspireId) {
        requireOwnedSeries(userId, seriesId);
        InspireMain article = mainMapper.selectById(inspireId);
        if (article == null || Integer.valueOf(1).equals(article.getDeleted())
                || !Integer.valueOf(1).equals(article.getStatus())
                || !userId.equals(article.getUserId())) {
            throw new BusinessException("只能添加自己的已发布灵感");
        }
        if (article.getSeriesId() != null) {
            if (seriesId.equals(article.getSeriesId())) {
                return getMySeries(userId, seriesId);
            }
            throw new BusinessException("该灵感已经在其他系列中");
        }
        Long count = mainMapper.selectCount(Wrappers.lambdaQuery(InspireMain.class)
                .eq(InspireMain::getSeriesId, seriesId)
                .eq(InspireMain::getUserId, userId)
                .eq(InspireMain::getDeleted, 0)
                .eq(InspireMain::getStatus, 1));
        if (count != null && count >= maxArticlesPerSeries) {
            throw new BusinessException("单个系列最多包含 " + maxArticlesPerSeries + " 篇灵感");
        }
        Integer nextOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(series_order), 0) + 1 FROM inspire_main "
                        + "WHERE series_id = ? AND user_id = ? AND deleted = 0 AND status = 1",
                Integer.class, seriesId, userId);
        jdbcTemplate.update(
                "UPDATE inspire_main SET series_id = ?, series_order = ? WHERE id = ? AND user_id = ?",
                seriesId, nextOrder == null ? 1 : nextOrder, inspireId, userId);
        refreshSeriesCover(seriesId, userId);
        touchSeries(seriesId);
        return getMySeries(userId, seriesId);
    }

    @Override
    @Transactional
    public SeriesVO removeSeriesArticle(Long userId, Long seriesId, Long inspireId) {
        requireOwnedSeries(userId, seriesId);
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE id = ? AND series_id = ? AND user_id = ?",
                Integer.class, inspireId, seriesId, userId);
        if (exists == null || exists == 0) {
            throw new BusinessException("该灵感不在当前系列中");
        }
        jdbcTemplate.update(
                "UPDATE inspire_main SET series_id = NULL, series_order = 0 WHERE id = ? AND user_id = ?",
                inspireId, userId);
        List<Long> remainingIds = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                        .eq(InspireMain::getSeriesId, seriesId)
                        .eq(InspireMain::getUserId, userId)
                        .eq(InspireMain::getDeleted, 0)
                        .eq(InspireMain::getStatus, 1)
                        .orderByAsc(InspireMain::getSeriesOrder)
                        .orderByAsc(InspireMain::getId))
                .stream().map(InspireMain::getId).collect(Collectors.toList());
        applySeriesOrder(seriesId, userId, remainingIds);
        refreshSeriesCover(seriesId, userId);
        touchSeries(seriesId);
        return getMySeries(userId, seriesId);
    }

    @Override
    @Transactional
    public SeriesVO reorderSeriesArticles(Long userId, Long seriesId, List<Long> articleIds) {
        requireOwnedSeries(userId, seriesId);
        if (articleIds == null || articleIds.isEmpty()) {
            throw new BusinessException("系列文章不能为空");
        }
        if (articleIds.size() > maxArticlesPerSeries) {
            throw new BusinessException("单个系列最多包含 " + maxArticlesPerSeries + " 篇灵感");
        }
        if (new HashSet<>(articleIds).size() != articleIds.size()) {
            throw new BusinessException("系列文章顺序中存在重复项");
        }
        List<Long> currentIds = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                        .select(InspireMain::getId)
                        .eq(InspireMain::getSeriesId, seriesId)
                        .eq(InspireMain::getUserId, userId)
                        .eq(InspireMain::getDeleted, 0)
                        .eq(InspireMain::getStatus, 1))
                .stream().map(InspireMain::getId).collect(Collectors.toList());
        if (currentIds.size() != articleIds.size() || !new HashSet<>(currentIds).equals(new HashSet<>(articleIds))) {
            throw new BusinessException("系列文章已发生变化，请刷新后重试");
        }
        applySeriesOrder(seriesId, userId, articleIds);
        refreshSeriesCover(seriesId, userId);
        touchSeries(seriesId);
        return getMySeries(userId, seriesId);
    }

    @Override
    public PageResult<InspireVO> listSeriesCandidates(Long userId, Long seriesId, String keyword, int page, int size) {
        requireOwnedSeries(userId, seriesId);
        int safeSize = Math.max(1, Math.min(size, 50));
        LambdaQueryWrapper<InspireMain> wrapper = Wrappers.lambdaQuery(InspireMain.class)
                .eq(InspireMain::getUserId, userId)
                .eq(InspireMain::getStatus, 1)
                .eq(InspireMain::getDeleted, 0)
                .isNull(InspireMain::getSeriesId)
                .orderByDesc(InspireMain::getCreateTime)
                .orderByDesc(InspireMain::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(InspireMain::getTitle, keyword.trim());
        }
        Page<InspireMain> result = mainMapper.selectPage(new Page<>(Math.max(1, page), safeSize), wrapper);
        return new PageResult<>(toVOList(result.getRecords(), userId), result.getTotal());
    }

    private void fillSeriesContext(InspireVO vo, InspireMain main) {
        if (main.getSeriesId() == null) return;
        InspireSeries series = seriesMapper.selectById(main.getSeriesId());
        if (series == null) return;
        List<InspireMain> articles = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .eq(InspireMain::getSeriesId, main.getSeriesId())
                .eq(InspireMain::getDeleted, 0)
                .eq(InspireMain::getStatus, 1)
                .orderByAsc(InspireMain::getSeriesOrder));
        vo.setSeriesId(String.valueOf(series.getId()));
        vo.setSeriesName(series.getName());
        vo.setSeriesOrder(main.getSeriesOrder());
        vo.setSeriesTotal(articles.size());
        for (int i = 0; i < articles.size(); i++) {
            if (!articles.get(i).getId().equals(main.getId())) continue;
            if (i > 0) {
                vo.setPrevSeriesId(String.valueOf(articles.get(i - 1).getId()));
                vo.setPrevSeriesTitle(articles.get(i - 1).getTitle());
            }
            if (i + 1 < articles.size()) {
                vo.setNextSeriesId(String.valueOf(articles.get(i + 1).getId()));
                vo.setNextSeriesTitle(articles.get(i + 1).getTitle());
            }
            break;
        }
    }

    private InspireSeries requireOwnedSeries(Long userId, Long seriesId) {
        checkUserExists(userId);
        InspireSeries series = seriesMapper.selectById(seriesId);
        if (series == null || Integer.valueOf(1).equals(series.getDeleted())
                || !Integer.valueOf(1).equals(series.getStatus())
                || !userId.equals(series.getUserId())) {
            throw new BusinessException("系列不存在或无权管理");
        }
        return series;
    }

    private List<InspireMain> loadSeriesArticles(Long seriesId) {
        return mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .eq(InspireMain::getSeriesId, seriesId)
                .eq(InspireMain::getDeleted, 0)
                .eq(InspireMain::getStatus, 1)
                .orderByAsc(InspireMain::getSeriesOrder)
                .orderByAsc(InspireMain::getId));
    }

    private SeriesVO buildSeriesVO(InspireSeries series, List<InspireMain> articles, Long loginUserId) {
        SeriesVO vo = new SeriesVO();
        vo.setId(series.getId());
        vo.setUserId(series.getUserId());
        vo.setName(series.getName());
        vo.setDescription(series.getDescription());
        vo.setCover(series.getCover());
        vo.setUpdateTime(series.getUpdateTime());
        vo.setTotal(articles.size());
        vo.setArticles(toVOList(articles, loginUserId));
        return vo;
    }

    private void applySeriesOrder(Long seriesId, Long userId, List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) return;
        StringBuilder sql = new StringBuilder("UPDATE inspire_main SET series_order = CASE id");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < articleIds.size(); i++) {
            sql.append(" WHEN ? THEN ?");
            params.add(articleIds.get(i));
            params.add(i + 1);
        }
        String placeholders = articleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        sql.append(" END WHERE series_id = ? AND user_id = ? AND id IN (").append(placeholders).append(")");
        params.add(seriesId);
        params.add(userId);
        params.addAll(articleIds);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    private void refreshSeriesCover(Long seriesId, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT img FROM inspire_main WHERE series_id = ? AND user_id = ? "
                        + "AND deleted = 0 AND status = 1 ORDER BY series_order, id LIMIT 1",
                seriesId, userId);
        String cover = rows.isEmpty() ? "" : Objects.toString(rows.get(0).get("img"), "");
        jdbcTemplate.update("UPDATE inspire_series SET cover = ?, update_time = ? WHERE id = ?",
                cover, LocalDateTime.now(), seriesId);
    }

    private void touchSeries(Long seriesId) {
        jdbcTemplate.update("UPDATE inspire_series SET update_time = ? WHERE id = ?",
                LocalDateTime.now(), seriesId);
    }

    private String normalizeSeriesName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new BusinessException("请输入系列名称");
        }
        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    private String normalizeSeriesDescription(String description) {
        String value = description == null ? "" : description.trim();
        return value.length() > 300 ? value.substring(0, 300) : value;
    }

    @Override
    public PageResult<InspireVO> listMyPublished(Long userId, int page, int size, String cursor) {
        return queryByUser(userId, 1, page, size, cursor);
    }

    @Override
    public PageResult<InspireVO> listMyDrafts(Long userId, int page, int size, String cursor) {
        return queryByUser(userId, 0, page, size, cursor);
    }

    private PageResult<InspireVO> queryByUser(Long userId, int status, int page, int size, String cursor) {
        int safeSize = Math.max(1, Math.min(size, 50));
        if ((cursor == null || cursor.isBlank()) && page > 1) {
            LambdaQueryWrapper<InspireMain> pageQuery = Wrappers.lambdaQuery();
            pageQuery.eq(InspireMain::getUserId, userId)
                    .eq(InspireMain::getStatus, status)
                    .eq(InspireMain::getDeleted, 0)
                    .orderByDesc(InspireMain::getCreateTime)
                    .orderByDesc(InspireMain::getId);
            Page<InspireMain> mpPage = mainMapper.selectPage(new Page<>(page, safeSize), pageQuery);
            return new PageResult<>(toVOList(mpPage.getRecords(), userId), mpPage.getTotal());
        }

        long total = 0;
        LambdaQueryWrapper<InspireMain> w = Wrappers.lambdaQuery();
        w.eq(InspireMain::getUserId, userId)
                .eq(InspireMain::getStatus, status)
                .eq(InspireMain::getDeleted, 0);
        if (cursor == null || cursor.isBlank()) {
            total = mainMapper.selectCount(w);
        } else {
            parseCursor(cursor).ifPresent(point -> w.and(q -> q
                    .lt(InspireMain::getCreateTime, point.time())
                    .or(n -> n.eq(InspireMain::getCreateTime, point.time())
                            .lt(InspireMain::getId, point.id()))));
        }
        w.orderByDesc(InspireMain::getCreateTime)
                .orderByDesc(InspireMain::getId)
                .last("LIMIT " + (safeSize + 1));
        List<InspireMain> rows = mainMapper.selectList(w);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, safeSize));
        String nextCursor = null;
        if (!rows.isEmpty()) {
            InspireMain last = rows.get(rows.size() - 1);
            if (last.getCreateTime() != null) {
                nextCursor = last.getCreateTime() + "_" + last.getId();
            }
        }
        return new PageResult<>(toVOList(rows, userId), total, nextCursor, hasMore);
    }

    private Optional<CursorPoint> parseCursor(String cursor) {
        int split = cursor.lastIndexOf('_');
        if (split <= 0 || split >= cursor.length() - 1) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CursorPoint(
                    LocalDateTime.parse(cursor.substring(0, split)),
                    Long.parseLong(cursor.substring(split + 1))));
        } catch (Exception e) {
            log.debug("忽略无效分页游标: {}", cursor);
            return Optional.empty();
        }
    }

    private record CursorPoint(LocalDateTime time, Long id) {
    }

    @Override @Transactional
    public InspireMain create(InspireCreateRequest req, Long userId) {
        checkUserExists(userId);
        String title = TitleUtil.truncate(req.getTitle());
        InspireMain m = new InspireMain();
        m.setId(nextId()); m.setTitle(title);
        m.setImg(req.getImg() != null ? req.getImg() : ""); m.setImages(req.getImages() != null ? req.getImages() : ""); m.setTag(req.getTag()); m.setUserId(userId);
        // 内容审核：命中敏感词设为待审核（2），否则用请求的status
        String reason = TextFilter.check(title);
        if (reason == null) {
            reason = TextFilter.check(req.getContent());
        }
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
        if (m == null || m.getDeleted() == 1) {
            throw new BusinessException("灵感不存在");
        }
        if (!m.getUserId().equals(userId)) {
            throw new BusinessException("只能修改自己的灵感");
        }
        if (req.getTitle() != null) {
            m.setTitle(TitleUtil.truncate(req.getTitle()));
        }
        if (req.getTag() != null) {
            m.setTag(req.getTag());
        }
        if (req.getImg() != null) {
            m.setImg(req.getImg());
        }
        if (req.getImages() != null) {
            m.setImages(req.getImages());
        }
        if (req.getStatus() != null) {
            m.setStatus(req.getStatus());
        }
        if (req.getPublishCity() != null) {
            m.setPublishCity(req.getPublishCity());
        }
        
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
        if (m == null || m.getDeleted() == 1) {
            return;
        }
        if (!m.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的灵感");
        }
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
        if (folderId == null) {
            folderId = ensureUnclassifiedFolder(userId).getId();
        }
        ShardContext.setByUserId(userId);
        try {
            if (collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, userId).eq(CollectAction::getInspireId, inspireId)) != null) {
                throw new BusinessException("已收藏");
            }
            CollectAction a = new CollectAction(); a.setId(nextId()); a.setUserId(userId); a.setInspireId(inspireId);
            a.setFolderId(folderId);
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
            if (myName == null) {
                myName = String.valueOf(userId);
            }
            String title = inspireForMsg != null ? inspireForMsg.getTitle() : "";
            if (title != null && title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            if (inspireForMsg != null) {
                notificationService.notify(inspireForMsg.getUserId(), "collect", userId,
                    myName, "收藏了你的灵感", inspireId, title);
            }
        } catch (Exception e) { log.warn("收藏通知发送失败", e); }
    }

    /** 没有显式选择文件夹时，自动把收藏放进真实存在的「未分类」文件夹。 */
    private CollectFolder ensureUnclassifiedFolder(Long userId) {
        CollectFolder folder = collectFolderMapper.selectOne(Wrappers.lambdaQuery(CollectFolder.class)
                .eq(CollectFolder::getUserId, userId)
                .eq(CollectFolder::getName, "未分类")
                .last("LIMIT 1"));
        if (folder != null) {
            return folder;
        }
        CollectFolder created = new CollectFolder();
        created.setId(nextId());
        created.setUserId(userId);
        created.setName("未分类");
        created.setIcon("📂");
        created.setSortOrder(999);
        created.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        collectFolderMapper.insert(created);
        return created;
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
        ShardContext.setByUserId(userId);
        try {
            if (likeMapper.selectOne(Wrappers.lambdaQuery(LikeAction.class)
                    .eq(LikeAction::getInspireId, inspireId).eq(LikeAction::getUserId, userId)) != null) {
                throw new BusinessException("已点赞");
            }
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
            if (myName == null) {
                myName = String.valueOf(userId);
            }
            String title = inspireForMsg != null ? inspireForMsg.getTitle() : "";
            if (title != null && title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            if (inspireForMsg != null) {
                notificationService.notify(inspireForMsg.getUserId(), "like", userId,
                    myName, "点赞了你的灵感", inspireId, title);
            }
        } catch (Exception e) { log.warn("点赞通知发送失败", e); }
    }

    @Override @Transactional
    public void unlike(Long userId, Long inspireId) {
        checkUserExists(userId);

        ShardContext.setByUserId(userId);
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
            log.debug("[PAGEDBG] listMyCollects userId={} page={} size={} totalCollects={}", userId, page, size, total);
            int start = (page - 1) * size;
            if (start >= collects.size()) {
                log.debug("[PAGEDBG] start={} >= collects.size={} -> empty", start, collects.size());
                return new PageResult<>(new ArrayList<>(), total);
            }
            int end = Math.min(start + size, collects.size());
            log.debug("[PAGEDBG] start={} end={} records={}", start, end, end - start);
            collects = collects.subList(start, end);
            if (collects.isEmpty()) {
                return new PageResult<>(new ArrayList<>(), total);
            }
            ids = collects.stream().map(CollectAction::getInspireId).collect(Collectors.toList());
        } finally { ShardContext.clear(); }
        List<InspireMain> mains = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .in(InspireMain::getId, ids).eq(InspireMain::getDeleted, 0));
        log.debug("[PAGEDBG] found mains={} total={}", mains.size(), total);
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

    @Override
    public List<CollectFolder> getCollectFolders(Long userId) {
        List<CollectFolder> folders = collectFolderMapper.selectList(Wrappers.lambdaQuery(CollectFolder.class)
                .eq(CollectFolder::getUserId, userId).orderByAsc(CollectFolder::getSortOrder));
        int shard = (int) Math.floorMod(userId, 10);
        Integer nullFolderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collect_" + shard
                        + " WHERE user_id = ? AND folder_id IS NULL",
                Integer.class, userId);
        if (nullFolderCount != null && nullFolderCount > 0) {
            CollectFolder uncategorized = folders.stream()
                    .filter(f -> "未分类".equals(f.getName()))
                    .findFirst()
                    .orElse(null);
            if (uncategorized == null) {
                uncategorized = new CollectFolder();
                uncategorized.setId(nextId());
                uncategorized.setUserId(userId);
                uncategorized.setName("未分类");
                uncategorized.setIcon("📂");
                uncategorized.setSortOrder(999);
                uncategorized.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
                collectFolderMapper.insert(uncategorized);
                folders.add(uncategorized);
            }
            jdbcTemplate.update(
                    "UPDATE collect_" + shard + " SET folder_id = ? "
                            + "WHERE user_id = ? AND folder_id IS NULL",
                    uncategorized.getId(), userId);
        }
        if (folders.isEmpty()) {
            return folders;
        }

        Map<Long, Integer> counts = new HashMap<>();
        try {
            jdbcTemplate.query(
                    "SELECT folder_id, COUNT(*) AS c FROM collect_" + shard
                            + " WHERE user_id = ? AND folder_id IS NOT NULL GROUP BY folder_id",
                    rs -> { counts.put(rs.getLong("folder_id"), rs.getInt("c")); },
                    userId);
        } catch (Exception e) {
            log.warn("批量统计收藏夹数量失败: userId={}", userId, e);
        }
        folders.forEach(folder -> folder.setCount(counts.getOrDefault(folder.getId(), 0)));
        return folders;
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
        if (f == null || !f.getUserId().equals(userId)) {
            throw new BusinessException("文件夹不存在");
        }
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
        if (f == null || !f.getUserId().equals(userId)) {
            throw new BusinessException("文件夹不存在");
        }
        f.setName(name);
        collectFolderMapper.updateById(f);
    }

    @Override
    public PageResult<InspireVO> listCollectsByFolder(Long userId, Long folderId, int page, int size) {
        ShardContext.setByUserId(userId);
        Page<CollectAction> collectPage;
        try {
            if (folderId != null && folderId > 0) {
                collectPage = collectMapper.selectPage(new Page<>(page, size), Wrappers.lambdaQuery(CollectAction.class)
                        .eq(CollectAction::getUserId, userId)
                        .eq(CollectAction::getFolderId, folderId)
                        .orderByDesc(CollectAction::getCreateTime));
            } else {
                collectPage = collectMapper.selectPage(new Page<>(page, size), Wrappers.lambdaQuery(CollectAction.class)
                        .eq(CollectAction::getUserId, userId)
                        .isNull(CollectAction::getFolderId)
                        .orderByDesc(CollectAction::getCreateTime));
            }
        } finally { ShardContext.clear(); }

        List<CollectAction> collects = collectPage.getRecords();
        if (collects.isEmpty()) {
            return new PageResult<>(List.of(), collectPage.getTotal());
        }
        List<Long> ids = collects.stream().map(CollectAction::getInspireId).collect(Collectors.toList());
        List<InspireMain> mains = mainMapper.selectList(Wrappers.lambdaQuery(InspireMain.class)
                .in(InspireMain::getId, ids));
        Map<Long, InspireMain> map = mains.stream().collect(Collectors.toMap(InspireMain::getId, m -> m));
        Map<Long, String> nicknameMap = buildNicknameMap(mains);
        Set<Long> likedIds = buildRelationState(userId, ids).likedIds();

        List<InspireVO> result = new ArrayList<>();
        for (CollectAction c : collects) {
            InspireMain m = map.get(c.getInspireId());
            if (m != null) {
                InspireVO vo = singleToVO(m, nicknameMap.get(m.getUserId()), null);
                vo.setCollected(true);
                vo.setLiked(likedIds.contains(m.getId()));
                result.add(vo);
            }
        }
        return new PageResult<>(result, collectPage.getTotal());
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
        if (mains.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Batch query nicknames
        Map<Long, String> nicknameMap = buildNicknameMap(mains);

        // 2. Batch query collect/like status
        Set<Long> collectedIds = new HashSet<>();
        Set<Long> likedIds = new HashSet<>();
        if (loginUserId != null) {
            List<Long> inspireIds = mains.stream().map(InspireMain::getId).collect(Collectors.toList());
            UserRelationState relationState = buildRelationState(loginUserId, inspireIds);
            collectedIds = relationState.collectedIds();
            likedIds = relationState.likedIds();
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
        if (userIds.isEmpty()) {
            return map;
        }
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

    /**
     * 收藏和点赞都按 user_id 分片，合并为一次 UNION ALL 查询，减少列表接口一次数据库往返。
     */
    private UserRelationState buildRelationState(Long userId, List<Long> inspireIds) {
        if (inspireIds.isEmpty()) {
            return new UserRelationState(Collections.emptySet(), Collections.emptySet());
        }
        int shard = Math.floorMod(userId, 10);
        String placeholders = inspireIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT 'collect' AS kind, inspire_id FROM collect_" + shard
                + " WHERE user_id = ? AND inspire_id IN (" + placeholders + ")"
                + " UNION ALL "
                + "SELECT 'like' AS kind, inspire_id FROM user_like_" + shard
                + " WHERE user_id = ? AND inspire_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(inspireIds);
        params.add(userId);
        params.addAll(inspireIds);

        Set<Long> collectedIds = new HashSet<>();
        Set<Long> likedIds = new HashSet<>();
        try {
            jdbcTemplate.query(sql, rs -> {
                long inspireId = rs.getLong("inspire_id");
                if ("collect".equals(rs.getString("kind"))) {
                    collectedIds.add(inspireId);
                } else {
                    likedIds.add(inspireId);
                }
            }, params.toArray());
        } catch (Exception e) {
            log.warn("批量查询收藏/点赞状态失败: userId={}", userId, e);
        }
        return new UserRelationState(collectedIds, likedIds);
    }

    /**
     * 详情专用转换：一次补齐作者头像和当前用户关注状态，
     * 避免前端进入详情后再额外请求用户信息和完整关注列表。
     */
    private InspireVO toDetailVO(InspireMain m, Long loginUserId, String content) {
        String nickname = "";
        String avatar = "";
        try {
            Map<String, Object> profile = jdbcTemplate.queryForMap(
                    "SELECT nickname, avatar FROM user WHERE id=?", m.getUserId());
            nickname = Objects.toString(profile.get("nickname"), "");
            avatar = Objects.toString(profile.get("avatar"), "");
        } catch (Exception ignored) {}

        InspireVO vo = singleToVO(m, nickname, content);
        vo.setAvatar(avatar);
        if (loginUserId == null) {
            return vo;
        }

        ShardContext.setByUserId(loginUserId);
        try {
            vo.setCollected(collectMapper.selectOne(Wrappers.lambdaQuery(CollectAction.class)
                    .eq(CollectAction::getUserId, loginUserId)
                    .eq(CollectAction::getInspireId, m.getId())) != null);
        } finally {
            ShardContext.clear();
        }

        ShardContext.setByUserId(loginUserId);
        try {
            vo.setLiked(likeMapper.selectOne(Wrappers.lambdaQuery(LikeAction.class)
                    .eq(LikeAction::getUserId, loginUserId)
                    .eq(LikeAction::getInspireId, m.getId())) != null);
        } finally {
            ShardContext.clear();
        }

        if (!loginUserId.equals(m.getUserId())) {
            try {
                Integer following = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND followee_id = ?",
                        Integer.class, loginUserId, m.getUserId());
                vo.setFollowing(following != null && following > 0);
            } catch (Exception e) {
                log.warn("查询详情关注状态失败: userId={}, authorId={}", loginUserId, m.getUserId(), e);
            }
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
        vo.setTag(m.getTag());
        vo.setCategoryId(m.getCategoryId());
        vo.setSubCategoryId(m.getSubCategoryId());
        if (m.getSeriesId() != null) vo.setSeriesId(String.valueOf(m.getSeriesId()));
        vo.setSeriesOrder(m.getSeriesOrder());
        vo.setViewCount(m.getViewCount()); vo.setHeat(m.getHeat());
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
