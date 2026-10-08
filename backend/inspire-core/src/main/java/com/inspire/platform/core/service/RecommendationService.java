/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/RecommendationService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：多路召回、语义排序、行为反馈和稳定 A/B 分桶
 * 维护说明：推荐规则、实验和向量索引变化时同步维护本服务与推荐说明文档。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.entity.InspireMain;
import com.inspire.platform.core.mapper.InspireMainMapper;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int CANDIDATE_LIMIT = 500;
    private static final int VECTOR_RECALL_LIMIT = 300;
    private static final int MAX_TAG_PER_STREAM = 2;
    private static final Set<String> EVENT_TYPES = Set.of(
            "IMPRESSION", "CLICK", "DETAIL", "COLLECT", "LIKE", "SKIP", "NEGATIVE", "DWELL"
    );
    private static final Set<String> POSITIVE_EVENTS = Set.of(
            "CLICK", "DETAIL", "COLLECT", "LIKE"
    );

    private final InspireMainMapper inspireMainMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MqProducer mqProducer;
    private final RecommendationVectorService vectorService;
    private final RecommendationConfigService configService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${inspire.recommend.experiment-enabled:true}")
    private boolean experimentEnabled;

    @Value("${inspire.recommend.experiment-id:recommend_v2}")
    private String experimentId;

    @Value("${inspire.recommend.experiment-treatment-percent:50}")
    private int treatmentPercent;

    @Value("${inspire.recommend.redis-profile-ttl-seconds:604800}")
    private long profileTtlSeconds;

    @Value("${inspire.recommend.redis-vector-ttl-seconds:1800}")
    private long vectorTtlSeconds;

    @Value("${inspire.recommend.embedding-dims:768}")
    private int embeddingDims;

    public RecommendationResult recommend(Long userId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 20));
        RecommendationConfigService.RecommendationRuntimeConfig runtimeConfig = configService.current();
        ExperimentAssignment assignment = assign(userId, runtimeConfig);
        UserProfile profile = userId == null ? UserProfile.empty() : loadProfile(userId);
        Set<Long> excluded = userId == null ? Set.of() : loadRecentNegativeIds(userId);

        RecallResult recall = recall(userId, profile, assignment, runtimeConfig, safePage, safeSize);
        Map<Long, Double> semanticScores = recall.semanticScores();
        List<Scored> ranked = rank(recall.candidates(), profile, semanticScores,
                recall.pushCandidates(), excluded, assignment.isTreatment());
        List<Scored> diversified = diversify(ranked);
        int from = Math.min((safePage - 1) * safeSize, diversified.size());
        int to = Math.min(from + safeSize, diversified.size());
        return new RecommendationResult(
                diversified.subList(from, to),
                assignment.experimentId(),
                assignment.variant()
        );
    }

    public void recordEvent(Long userId, Long inspireId, String rawType,
                            String reasonCode, String experiment, String variant,
                            Integer durationMs, Long pushId) {
        if (userId == null || inspireId == null) {
            throw new BusinessException(400, "推荐行为缺少用户或内容");
        }
        String type = rawType == null ? "" : rawType.trim().toUpperCase(Locale.ROOT);
        if (!EVENT_TYPES.contains(type)) {
            throw new BusinessException(400, "不支持的推荐行为");
        }
        RecommendationEventMessage message = new RecommendationEventMessage(
                userId,
                inspireId,
                type,
                abbreviate(reasonCode, 40),
                abbreviate(experiment, 40),
                abbreviate(variant, 20),
                durationMs == null ? 0 : Math.max(0, durationMs),
                pushId
        );
        if (!mqProducer.send(MqTopicConstants.TOPIC_RECOMMEND_EVENT, message)) {
            persistEvent(message);
        }
    }

    public void persistEvent(RecommendationEventMessage event) {
        if (event == null || event.userId() == null || event.inspireId() == null) return;
        jdbcTemplate.update("""
                INSERT INTO recommend_event
                    (user_id,inspire_id,event_type,reason_code,experiment_id,variant,duration_ms,push_id)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                event.userId(),
                event.inspireId(),
                event.eventType(),
                event.reasonCode(),
                event.experimentId(),
                event.variant(),
                event.durationMs(),
                event.pushId()
        );
        applyEventToProfile(event);
    }

    public List<Map<String, Object>> metrics(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        return jdbcTemplate.queryForList("""
                SELECT experiment_id,
                       variant,
                       SUM(event_type = 'IMPRESSION') AS impressions,
                       SUM(event_type = 'CLICK') AS clicks,
                       SUM(event_type = 'COLLECT') AS collects,
                       SUM(event_type = 'SKIP') AS skips,
                       SUM(event_type = 'DWELL') AS dwell_events,
                       ROUND(AVG(CASE WHEN event_type = 'DWELL' THEN duration_ms END), 0) AS avg_dwell_ms
                FROM recommend_event
                WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)
                GROUP BY experiment_id, variant
                ORDER BY experiment_id, variant
                """, safeDays);
    }

    public ExperimentAssignment assign(Long userId,
                                       RecommendationConfigService.RecommendationRuntimeConfig runtimeConfig) {
        if (userId == null || !experimentEnabled) {
            return new ExperimentAssignment("control", "ANON", false);
        }
        int effectiveTreatmentPercent = runtimeConfig == null
                ? treatmentPercent : runtimeConfig.treatmentPercent();
        int controlPercent = Math.max(0, Math.min(100, 100 - effectiveTreatmentPercent));
        int bucket = Math.floorMod(Objects.hash(userId, experimentId), 100);
        boolean treatment = bucket >= controlPercent;
        return new ExperimentAssignment(
                experimentId,
                treatment ? "TREATMENT" : "CONTROL",
                treatment
        );
    }

    private RecallResult recall(Long userId, UserProfile profile,
                                ExperimentAssignment assignment,
                                RecommendationConfigService.RecommendationRuntimeConfig runtimeConfig,
                                int page, int size) {
        Map<Long, InspireMain> merged = new LinkedHashMap<>();
        int limit = Math.min(CANDIDATE_LIMIT, Math.max(160, page * size * 14));
        List<InspireMain> hot = inspireMainMapper.selectList(
                Wrappers.<InspireMain>lambdaQuery()
                        .eq(InspireMain::getStatus, 1)
                        .eq(InspireMain::getDeleted, 0)
                        .orderByDesc(InspireMain::getHeat)
                        .orderByDesc(InspireMain::getCreateTime)
                        .last("LIMIT " + limit));
        hot.forEach(item -> merged.put(item.getId(), item));

        if (!profile.tags().isEmpty()) {
            List<String> tags = profile.tags().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(4)
                    .map(Map.Entry::getKey)
                    .toList();
            inspireMainMapper.selectList(Wrappers.<InspireMain>lambdaQuery()
                    .eq(InspireMain::getStatus, 1)
                    .eq(InspireMain::getDeleted, 0)
                    .in(InspireMain::getTag, tags)
                    .orderByDesc(InspireMain::getHeat)
                    .last("LIMIT 180"))
                    .forEach(item -> merged.putIfAbsent(item.getId(), item));
        }

        if (!profile.authors().isEmpty()) {
            List<Long> authors = profile.authors().entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(8)
                    .map(Map.Entry::getKey)
                    .toList();
            inspireMainMapper.selectList(Wrappers.<InspireMain>lambdaQuery()
                    .eq(InspireMain::getStatus, 1)
                    .eq(InspireMain::getDeleted, 0)
                    .in(InspireMain::getUserId, authors)
                    .orderByDesc(InspireMain::getCreateTime)
                    .last("LIMIT 120"))
                    .forEach(item -> merged.putIfAbsent(item.getId(), item));
        }

        Map<Long, Double> semanticScores = Map.of();
        if (assignment.isTreatment() && runtimeConfig.vectorRecallEnabled() && userId != null) {
            float[] userVector = loadUserVector(userId);
            if (userVector != null) {
                semanticScores = vectorService.search(userVector, VECTOR_RECALL_LIMIT);
                if (!semanticScores.isEmpty()) {
                    List<InspireMain> vectors = inspireMainMapper.selectBatchIds(semanticScores.keySet());
                    vectors.stream()
                            .filter(item -> Integer.valueOf(1).equals(item.getStatus())
                                    && !Integer.valueOf(1).equals(item.getDeleted()))
                            .forEach(item -> merged.putIfAbsent(item.getId(), item));
                }
            }
        }
        Map<Long, PushCandidate> pushCandidates =
                loadActivePushes(userId, profile, runtimeConfig);
        for (PushCandidate push : pushCandidates.values()) {
            InspireMain item = inspireMainMapper.selectById(push.inspireId());
            if (item != null && Integer.valueOf(1).equals(item.getStatus())
                    && !Integer.valueOf(1).equals(item.getDeleted())) {
                merged.putIfAbsent(item.getId(), item);
            }
        }
        return new RecallResult(new ArrayList<>(merged.values()), semanticScores, pushCandidates);
    }

    private List<Scored> rank(List<InspireMain> candidates, UserProfile profile,
                              Map<Long, Double> semanticScores,
                              Map<Long, PushCandidate> pushCandidates,
                              Set<Long> excluded, boolean treatment) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        double maxLogHeat = candidates.stream()
                .mapToDouble(item -> Math.log1p(Math.max(0, item.getHeat() == null ? 0 : item.getHeat())))
                .max().orElse(1d);
        double maxTagWeight = profile.tags.values().stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        double maxAuthorWeight = profile.authors.values().stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        double maxSemantic = semanticScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        double minSemantic = semanticScores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0d);

        List<Scored> result = new ArrayList<>();
        for (InspireMain item : candidates) {
            if (excluded.contains(item.getId())) continue;
            String tag = item.getTag() == null ? "" : item.getTag();
            double interest = maxTagWeight <= 0 ? 0 : profile.tags.getOrDefault(tag, 0d) / maxTagWeight;
            double author = item.getUserId() == null || maxAuthorWeight <= 0
                    ? 0 : profile.authors.getOrDefault(item.getUserId(), 0d) / maxAuthorWeight;
            double heat = maxLogHeat <= 0 ? 0
                    : Math.log1p(Math.max(0, item.getHeat() == null ? 0 : item.getHeat())) / maxLogHeat;
            double freshness = freshness(item.getCreateTime());
            double semantic = treatment
                    ? normalizeSemantic(semanticScores.get(item.getId()), minSemantic, maxSemantic)
                    : 0;
            double negative = profile.negative().getOrDefault(tag, 0d);
            double novelty = profile.tags().isEmpty() ? 0.35
                    : (profile.tags().containsKey(tag) ? 0.15 : 1d);
            PushCandidate push = pushCandidates.get(item.getId());
            double score = interest * 0.30 + author * 0.15 + heat * 0.18
                    + freshness * 0.14 + semantic * 0.19 + novelty * 0.04
                    - negative * 0.65;
            if (push != null) score += 0.42 + Math.min(0.25, push.weight() * 0.20);
            result.add(new Scored(
                    item,
                    score,
                    semantic,
                    push == null
                            ? reasonFor(tag, profile, interest, author, freshness, semantic)
                            : "运营推荐",
                    push == null ? null : push.id()
            ));
        }
        result.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparing(scored -> scored.item().getCreateTime(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private List<Scored> diversify(List<Scored> ranked) {
        List<Scored> primary = new ArrayList<>();
        List<Scored> overflow = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Scored scored : ranked) {
            String tag = scored.item().getTag() == null ? "" : scored.item().getTag();
            int count = counts.getOrDefault(tag, 0);
            if (count < MAX_TAG_PER_STREAM) {
                primary.add(scored);
                counts.put(tag, count + 1);
            } else {
                overflow.add(scored);
            }
        }
        primary.addAll(overflow);
        return primary;
    }

    private Map<Long, PushCandidate> loadActivePushes(
            Long userId,
            UserProfile profile,
            RecommendationConfigService.RecommendationRuntimeConfig runtimeConfig) {
        Map<Long, Integer> usedToday = userId == null
                ? Map.of()
                : loadPushUsageToday(userId);
        LocalDateTime userCreatedAt = loadUserCreatedAt(userId);
        List<PushRow> pushes = jdbcTemplate.query("""
                SELECT id,inspire_id,target_type,target_value,weight
                FROM recommend_push
                WHERE status = 'ACTIVE'
                  AND (start_time IS NULL OR start_time <= NOW())
                  AND (end_time IS NULL OR end_time > NOW())
                ORDER BY weight DESC, id ASC
                LIMIT 50
                """, (rs, rowNum) -> new PushRow(
                rs.getLong("id"),
                rs.getLong("inspire_id"),
                rs.getString("target_type"),
                rs.getString("target_value"),
                rs.getDouble("weight")
        ));
        Map<Long, PushCandidate> result = new LinkedHashMap<>();
        int maxPerDay = Math.max(0, runtimeConfig.pushMaxPerDay());
        for (PushRow push : pushes) {
            if (maxPerDay > 0 && usedToday.getOrDefault(push.id(), 0) >= maxPerDay) continue;
            if (!matchesPush(push, userId, profile, userCreatedAt)) continue;
            PushCandidate candidate = new PushCandidate(
                    push.id(), push.inspireId(), push.weight());
            PushCandidate existing = result.get(push.inspireId());
            if (existing == null || existing.weight() < candidate.weight()) {
                result.put(push.inspireId(), candidate);
            }
        }
        return result;
    }

    private boolean matchesPush(PushRow push, Long userId, UserProfile profile,
                                LocalDateTime userCreatedAt) {
        String targetType = push.targetType() == null
                ? "ALL" : push.targetType().toUpperCase(Locale.ROOT);
        return switch (targetType) {
            case "ALL" -> true;
            case "TAG" -> profile.tags().containsKey(push.targetValue());
            case "NEW_USER" -> userCreatedAt != null
                    && userCreatedAt.isAfter(LocalDateTime.now().minusDays(7));
            default -> false;
        };
    }

    private Map<Long, Integer> loadPushUsageToday(Long userId) {
        Map<Long, Integer> result = new HashMap<>();
        jdbcTemplate.query("""
                SELECT push_id, COUNT(*) AS count
                FROM recommend_event
                WHERE user_id = ? AND push_id IS NOT NULL
                  AND create_time >= CURDATE()
                GROUP BY push_id
                """, (RowCallbackHandler) rs ->
                        result.put(rs.getLong("push_id"), rs.getInt("count")), userId);
        return result;
    }

    private LocalDateTime loadUserCreatedAt(Long userId) {
        if (userId == null) return null;
        return jdbcTemplate.query("""
                SELECT create_time FROM user WHERE id = ?
                """, rs -> rs.next() && rs.getTimestamp("create_time") != null
                ? rs.getTimestamp("create_time").toLocalDateTime()
                : null, userId);
    }

    private UserProfile loadProfile(Long userId) {
        UserProfile cached = loadProfileFromRedis(userId);
        if (cached != null) return cached;

        Map<String, Double> tags = new HashMap<>();
        Map<Long, Double> authors = new HashMap<>();
        Map<String, Double> negativeTags = new HashMap<>();
        jdbcTemplate.query("""
                SELECT i.tag, COUNT(*) AS weight
                FROM (
                    SELECT inspire_id FROM user_like WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id FROM collect WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id FROM recommend_event
                    WHERE user_id = ? AND event_type IN ('COLLECT', 'LIKE', 'DETAIL', 'CLICK')
                ) behavior
                JOIN inspire_main i ON i.id = behavior.inspire_id
                WHERE i.deleted = 0 AND i.tag IS NOT NULL AND i.tag <> ''
                GROUP BY i.tag
                """, (RowCallbackHandler) rs ->
                        tags.merge(rs.getString("tag"), rs.getDouble("weight"), Double::sum),
                userId, userId, userId);
        jdbcTemplate.query("""
                SELECT i.user_id, COUNT(*) AS weight
                FROM (
                    SELECT inspire_id FROM user_like WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id FROM collect WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id FROM recommend_event
                    WHERE user_id = ? AND event_type IN ('COLLECT', 'LIKE', 'DETAIL', 'CLICK')
                ) behavior
                JOIN inspire_main i ON i.id = behavior.inspire_id
                WHERE i.deleted = 0 AND i.user_id IS NOT NULL
                GROUP BY i.user_id
                """, (RowCallbackHandler) rs ->
                        authors.merge(rs.getLong("user_id"), rs.getDouble("weight"), Double::sum),
                userId, userId, userId);
        jdbcTemplate.query("""
                SELECT i.tag, COUNT(*) AS weight
                FROM recommend_event e
                JOIN inspire_main i ON i.id = e.inspire_id
                WHERE e.user_id = ? AND e.event_type IN ('SKIP', 'NEGATIVE')
                  AND i.deleted = 0 AND i.tag IS NOT NULL AND i.tag <> ''
                GROUP BY i.tag
                """, (RowCallbackHandler) rs ->
                        negativeTags.merge(rs.getString("tag"), rs.getDouble("weight"), Double::sum),
                userId);

        UserProfile profile = new UserProfile(
                normalize(tags),
                normalizeLong(authors),
                normalize(negativeTags)
        );
        saveProfileToRedis(userId, profile);
        return profile;
    }

    private UserProfile loadProfileFromRedis(Long userId) {
        try {
            String tagsKey = profileKey(userId, "tags");
            Map<Object, Object> tags = redisTemplate.opsForHash().entries(tagsKey);
            if (tags.isEmpty()) return null;
            Map<String, Double> tagValues = new HashMap<>();
            tags.forEach((key, value) -> tagValues.put(String.valueOf(key), Double.parseDouble(String.valueOf(value))));
            Map<String, Double> authorValues = new HashMap<>();
            redisTemplate.opsForHash().entries(profileKey(userId, "authors"))
                    .forEach((key, value) -> authorValues.put(String.valueOf(key), Double.parseDouble(String.valueOf(value))));
            Map<String, Double> negative = new HashMap<>();
            redisTemplate.opsForHash().entries(profileKey(userId, "negative"))
                    .forEach((key, value) -> negative.put(String.valueOf(key), Double.parseDouble(String.valueOf(value))));
            return new UserProfile(normalize(tagValues), normalizeStringLong(authorValues), normalize(negative));
        } catch (Exception e) {
            return null;
        }
    }

    private void saveProfileToRedis(Long userId, UserProfile profile) {
        try {
            String tagsKey = profileKey(userId, "tags");
            redisTemplate.delete(List.of(tagsKey, profileKey(userId, "authors"), profileKey(userId, "negative")));
            if (!profile.tags().isEmpty()) {
                Map<String, String> values = new HashMap<>();
                profile.tags().forEach((key, value) -> values.put(key, String.valueOf(value)));
                redisTemplate.opsForHash().putAll(tagsKey, values);
                redisTemplate.expire(tagsKey, Duration.ofSeconds(profileTtlSeconds));
            }
            if (!profile.authors().isEmpty()) {
                String key = profileKey(userId, "authors");
                Map<String, String> values = new HashMap<>();
                profile.authors().forEach((id, value) -> values.put(String.valueOf(id), String.valueOf(value)));
                redisTemplate.opsForHash().putAll(key, values);
                redisTemplate.expire(key, Duration.ofSeconds(profileTtlSeconds));
            }
            if (!profile.negative().isEmpty()) {
                String key = profileKey(userId, "negative");
                Map<String, String> values = new HashMap<>();
                profile.negative().forEach((keyName, value) -> values.put(keyName, String.valueOf(value)));
                redisTemplate.opsForHash().putAll(key, values);
                redisTemplate.expire(key, Duration.ofSeconds(profileTtlSeconds));
            }
        } catch (Exception e) {
            log.debug("推荐画像缓存写入失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private void applyEventToProfile(RecommendationEventMessage event) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT tag,user_id FROM inspire_main WHERE id = ?
                    """, event.inspireId());
            if (rows.isEmpty()) return;
            String tag = Objects.toString(rows.get(0).get("tag"), "");
            long authorId = ((Number) rows.get(0).get("user_id")).longValue();
            boolean positive = POSITIVE_EVENTS.contains(event.eventType())
                    || ("DWELL".equals(event.eventType()) && event.durationMs() >= 3000);
            boolean negative = "SKIP".equals(event.eventType())
                    || "NEGATIVE".equals(event.eventType())
                    || ("DWELL".equals(event.eventType()) && event.durationMs() > 0
                        && event.durationMs() < 1200);
            if (positive && !tag.isBlank()) {
                redisTemplate.opsForHash().increment(profileKey(event.userId(), "tags"), tag, 1);
                redisTemplate.expire(profileKey(event.userId(), "tags"), Duration.ofSeconds(profileTtlSeconds));
                redisTemplate.opsForHash().increment(profileKey(event.userId(), "authors"),
                        String.valueOf(authorId), 1);
                redisTemplate.expire(profileKey(event.userId(), "authors"), Duration.ofSeconds(profileTtlSeconds));
            }
            if (negative && !tag.isBlank()) {
                redisTemplate.opsForHash().increment(profileKey(event.userId(), "negative"), tag, 1);
                redisTemplate.expire(profileKey(event.userId(), "negative"), Duration.ofSeconds(profileTtlSeconds));
            }
            redisTemplate.delete(vectorKey(event.userId()));
        } catch (Exception e) {
            log.debug("推荐画像事件聚合失败: {}", e.getMessage());
        }
    }

    private float[] loadUserVector(Long userId) {
        String key = vectorKey(userId);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                JsonNode node = objectMapper.readTree(cached);
                if (node.isArray() && node.size() == vectorServiceDims()) {
                    float[] vector = new float[node.size()];
                    for (int i = 0; i < node.size(); i++) vector[i] = (float) node.get(i).asDouble();
                    return vector;
                }
            }
        } catch (Exception ignored) {
            // continue to rebuild
        }
        List<Long> positiveIds = jdbcTemplate.queryForList("""
                SELECT inspire_id FROM (
                    SELECT inspire_id,create_time FROM user_like WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id,create_time FROM collect WHERE user_id = ?
                    UNION ALL
                    SELECT inspire_id,create_time FROM recommend_event
                    WHERE user_id = ? AND event_type IN ('COLLECT', 'LIKE', 'DETAIL', 'CLICK')
                ) behavior
                ORDER BY create_time DESC
                LIMIT 80
                """, Long.class, userId, userId, userId);
        float[] vector = vectorService.average(positiveIds);
        if (vector != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vector),
                        Duration.ofSeconds(vectorTtlSeconds));
            } catch (Exception ignored) {
                // cache is optional
            }
        }
        return vector;
    }

    private Set<Long> loadRecentNegativeIds(Long userId) {
        return new HashSet<>(jdbcTemplate.query("""
                SELECT DISTINCT inspire_id FROM (
                    SELECT inspire_id,create_time FROM recommend_event
                    WHERE user_id = ? AND event_type IN ('SKIP', 'NEGATIVE')
                    ORDER BY create_time DESC
                    LIMIT 200
                ) recent
                """, (rs, rowNum) -> rs.getLong("inspire_id"), userId));
    }

    private String reasonFor(String tag, UserProfile profile,
                             double interest, double author, double freshness, double semantic) {
        if (semantic > 0.78 && interest <= 0.01) return "与你的兴趣方向相似";
        if (interest > 0.01) return "因为你常看「" + tag + "」";
        if (author > 0.01) return "因为你关注的创作者有新灵感";
        if (freshness > 0.75) return "发现新的灵感";
        return "近期热门";
    }

    private double normalizeSemantic(Double value, double min, double max) {
        if (value == null) return 0;
        if (max <= min) return 0.5;
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    private double freshness(LocalDateTime createTime) {
        if (createTime == null) return 0;
        long days = Math.max(0, Duration.between(createTime, LocalDateTime.now()).toDays());
        return Math.exp(-days / 12d);
    }

    private int vectorServiceDims() {
        return embeddingDims;
    }

    private String profileKey(Long userId, String suffix) {
        return "recommend:profile:" + userId + ":" + suffix;
    }

    private String vectorKey(Long userId) {
        return "recommend:vector:" + userId;
    }

    private Map<String, Double> normalize(Map<String, Double> values) {
        double max = values.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max <= 0) return Map.of();
        Map<String, Double> result = new HashMap<>();
        values.forEach((key, value) -> result.put(key, value / max));
        return result;
    }

    private Map<Long, Double> normalizeLong(Map<Long, Double> values) {
        double max = values.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max <= 0) return Map.of();
        Map<Long, Double> result = new HashMap<>();
        values.forEach((key, value) -> result.put(key, value / max));
        return result;
    }

    private Map<Long, Double> normalizeStringLong(Map<String, Double> values) {
        Map<Long, Double> result = new HashMap<>();
        values.forEach((key, value) -> {
            try {
                result.put(Long.parseLong(key), value);
            } catch (NumberFormatException ignored) {
                // skip malformed cache entry
            }
        });
        return normalizeLong(result);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) return "";
        String text = value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    public record RecommendationResult(
            List<Scored> items,
            String experimentId,
            String variant
    ) {
    }

    public record Scored(
            InspireMain item,
            double score,
            double semanticScore,
            String reason,
            Long pushId
    ) {
    }

    public record ExperimentAssignment(
            String experimentId,
            String variant,
            boolean isTreatment
    ) {
    }

    public record RecommendationEventMessage(
            Long userId,
            Long inspireId,
            String eventType,
            String reasonCode,
            String experimentId,
            String variant,
            int durationMs,
            Long pushId
    ) {
    }

    private record RecallResult(
            List<InspireMain> candidates,
            Map<Long, Double> semanticScores,
            Map<Long, PushCandidate> pushCandidates
    ) {
    }

    public record PushCandidate(
            Long id,
            Long inspireId,
            double weight
    ) {
    }

    private record PushRow(
            Long id,
            Long inspireId,
            String targetType,
            String targetValue,
            double weight
    ) {
    }

    private record UserProfile(
            Map<String, Double> tags,
            Map<Long, Double> authors,
            Map<String, Double> negative
    ) {
        private static UserProfile empty() {
            return new UserProfile(Map.of(), Map.of(), Map.of());
        }
    }
}
