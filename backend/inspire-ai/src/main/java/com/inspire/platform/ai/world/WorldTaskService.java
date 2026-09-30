/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldTaskService.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.world.WorldModels.GenerateChapterRequest;
import com.inspire.platform.ai.world.WorldModels.GenerateSeedRequest;
import com.inspire.platform.ai.world.WorldModels.WorldTaskView;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldTaskService {

    public static final String TYPE_SEED = "SEED";
    public static final String TYPE_CHAPTER = "CHAPTER";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorldTaskView createSeedTask(long userId, GenerateSeedRequest request, String idempotencyKey) {
        return createTask(userId, TYPE_SEED, normalizeIdempotencyKey(idempotencyKey, request),
                request, null, null, null);
    }

    @Transactional
    public WorldTaskView createChapterTask(long userId, GenerateChapterRequest request, String idempotencyKey) {
        return createTask(userId, TYPE_CHAPTER, normalizeIdempotencyKey(idempotencyKey, request),
                request, null, request.lineId(), request.branchId());
    }

    public WorldTaskView getRequired(long taskId, long userId) {
        TaskRow row = findById(taskId);
        if (row == null || row.userId() != userId) {
            throw new BusinessException(404, "生成任务不存在");
        }
        return row.toView(readJson(row.resultJson()));
    }

    public WorldTaskView get(long taskId) {
        TaskRow row = findById(taskId);
        return row == null ? null : row.toView(readJson(row.resultJson()));
    }

    public TaskExecution loadForExecution(long taskId) {
        TaskRow row = findById(taskId);
        if (row == null) return null;
        return new TaskExecution(
                row.id(),
                row.userId(),
                row.taskType(),
                row.seedId(),
                row.lineId(),
                row.branchId(),
                row.requestJson()
        );
    }

    @Transactional
    public boolean claim(long taskId) {
        int stale = jdbcTemplate.update("""
                UPDATE world_generation_task
                SET status = ?, progress = 0, error_message = '上次执行被中断，已重新排队'
                WHERE id = ? AND status = ? AND update_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)
                """, STATUS_PENDING, taskId, STATUS_RUNNING);
        int updated = jdbcTemplate.update("""
                UPDATE world_generation_task
                SET status = ?, progress = 5, error_message = NULL
                WHERE id = ? AND status = ?
                """, STATUS_RUNNING, taskId, STATUS_PENDING);
        return stale > 0 || updated > 0;
    }

    public void updateProgress(long taskId, int progress) {
        jdbcTemplate.update("""
                UPDATE world_generation_task
                SET progress = ?
                WHERE id = ? AND status = ?
                """, Math.max(0, Math.min(progress, 99)), taskId, STATUS_RUNNING);
    }

    @Transactional
    public void complete(long taskId, Object result, Long seedId, Long lineId, Long branchId) {
        jdbcTemplate.update("""
                UPDATE world_generation_task
                SET status = ?, progress = 100, result_json = ?, error_message = NULL,
                    seed_id = COALESCE(?, seed_id),
                    line_id = COALESCE(?, line_id),
                    branch_id = COALESCE(?, branch_id)
                WHERE id = ?
                """,
                STATUS_SUCCESS,
                writeJson(result),
                seedId,
                lineId,
                branchId,
                taskId
        );
    }

    public void fail(long taskId, String message) {
        jdbcTemplate.update("""
                UPDATE world_generation_task
                SET status = ?, progress = 100, error_message = ?
                WHERE id = ?
                """,
                STATUS_FAILED,
                truncate(message, 500),
                taskId
        );
    }

    public List<Long> findRecoverableTaskIds() {
        return jdbcTemplate.queryForList("""
                SELECT id
                FROM world_generation_task
                WHERE status = ?
                   OR (status = ? AND update_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE))
                ORDER BY create_time ASC
                LIMIT 100
                """, Long.class, STATUS_PENDING, STATUS_RUNNING);
    }

    private WorldTaskView createTask(long userId,
                                     String taskType,
                                     String idempotencyKey,
                                     Object request,
                                     Long seedId,
                                     Long lineId,
                                     Long branchId) {
        long taskId = nextId();
        try {
            jdbcTemplate.update("""
                    INSERT INTO world_generation_task
                    (id, task_type, status, user_id, seed_id, line_id, branch_id,
                     idempotency_key, progress, request_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
                    """,
                    taskId,
                    taskType,
                    STATUS_PENDING,
                    userId,
                    seedId,
                    lineId,
                    branchId,
                    truncate(idempotencyKey, 128),
                    writeJson(request)
            );
            return get(taskId);
        } catch (DuplicateKeyException e) {
            TaskRow existing = jdbcTemplate.query("""
                    SELECT *
                    FROM world_generation_task
                    WHERE user_id = ? AND task_type = ? AND idempotency_key = ?
                    """, rs -> rs.next() ? mapRow(rs, 0) : null,
                    userId,
                    taskType,
                    truncate(idempotencyKey, 128)
            );
            if (existing == null) throw e;
            log.debug("世界生成任务幂等命中 user={} type={} key={}", userId, taskType, idempotencyKey);
            return existing.toView(readJson(existing.resultJson()));
        }
    }

    private TaskRow findById(long taskId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM world_generation_task
                WHERE id = ?
                """, rs -> rs.next() ? mapRow(rs, 0) : null, taskId);
    }

    private TaskRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TaskRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("task_type"),
                rs.getString("status"),
                rs.getInt("progress"),
                nullableLong(rs, "seed_id"),
                nullableLong(rs, "line_id"),
                nullableLong(rs, "branch_id"),
                rs.getString("result_json"),
                rs.getString("error_message"),
                rs.getTimestamp("create_time"),
                rs.getTimestamp("update_time"),
                rs.getString("request_json")
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(500, "任务参数序列化失败");
        }
    }

    private JsonNode readJson(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        String text = value == null ? "" : value;
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String normalizeIdempotencyKey(String idempotencyKey, Object request) {
        if (StringUtils.hasText(idempotencyKey)) {
            return truncate(idempotencyKey.trim(), 128);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(writeJson(request).getBytes()));
        } catch (Exception e) {
            return Integer.toHexString(writeJson(request).hashCode());
        }
    }

    private static long nextId() {
        return WorldIds.next();
    }

    private record TaskRow(
            long id,
            long userId,
            String taskType,
            String status,
            int progress,
            Long seedId,
            Long lineId,
            Long branchId,
            String resultJson,
            String errorMessage,
            Timestamp createTime,
            Timestamp updateTime,
            String requestJson
    ) {
        private WorldTaskView toView(JsonNode result) {
            return new WorldTaskView(
                    Long.toString(id),
                    taskType,
                    status,
                    progress,
                    seedId == null ? null : Long.toString(seedId),
                    lineId == null ? null : Long.toString(lineId),
                    branchId == null ? null : Long.toString(branchId),
                    result,
                    errorMessage,
                    createTime == null ? null : createTime.toLocalDateTime().toString(),
                    updateTime == null ? null : updateTime.toLocalDateTime().toString()
            );
        }
    }

    public record TaskExecution(
            long taskId,
            long userId,
            String taskType,
            Long seedId,
            Long lineId,
            Long branchId,
            String requestJson
    ) {
    }
}
