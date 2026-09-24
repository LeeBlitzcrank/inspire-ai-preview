package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 冷数据归档：先复制到 archive 表，确认成功后删除源表记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataArchiveService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${inspire.archive.enabled:true}")
    private boolean enabled;

    @Value("${inspire.archive.retention-days:180}")
    private int retentionDays;

    @Value("${inspire.archive.batch-size:500}")
    private int batchSize;

    private record ArchiveTarget(String source, String archive, String timeColumn) {
    }

    private static final List<ArchiveTarget> TARGETS = List.of(
            new ArchiveTarget("message", "message_archive", "create_time"),
            new ArchiveTarget("user_notification", "user_notification_archive", "create_time"),
            new ArchiveTarget("web_vital_metric", "web_vital_metric_archive", "create_time"),
            new ArchiveTarget("ai_call_log", "ai_call_log_archive", "create_time"),
            new ArchiveTarget("user_ai_history", "user_ai_history_archive", "create_time")
    );

    @Scheduled(cron = "${inspire.archive.cron:0 0 3 * * ?}")
    public void archiveDaily() {
        if (!enabled) return;
        for (ArchiveTarget target : TARGETS) {
            try {
                archiveOne(target);
            } catch (Exception e) {
                log.warn("冷数据归档失败: table={}, {}", target.source(), e.getMessage());
            }
        }
    }

    protected void archiveOne(ArchiveTarget target) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM " + target.source() + " WHERE " + target.timeColumn()
                        + " < ? ORDER BY id LIMIT ?",
                Long.class, cutoff, batchSize);
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Object[] args = ids.toArray();
        int copied = jdbcTemplate.update(
                "INSERT IGNORE INTO " + target.archive()
                        + " SELECT * FROM " + target.source()
                        + " WHERE id IN (" + placeholders + ")",
                args);
        int deleted = jdbcTemplate.update(
                "DELETE FROM " + target.source() + " WHERE id IN (" + placeholders + ")",
                args);
        log.info("冷数据归档: {} -> {}, copied={}, deleted={}",
                target.source(), target.archive(), copied, deleted);
    }
}
