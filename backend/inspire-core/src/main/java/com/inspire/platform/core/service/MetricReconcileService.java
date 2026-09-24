package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将分散在明细表中的计数收敛到 inspire_metric 投影表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricReconcileService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean initialDone = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${inspire.metric.reconcile-delay-ms:600000}",
            initialDelayString = "${inspire.metric.reconcile-initial-delay-ms:30000}")
    public void reconcile() {
        try {
            if (initialDone.compareAndSet(false, true)) {
                reconcileAll();
                return;
            }
            reconcileRecent();
        } catch (Exception e) {
            initialDone.set(false);
            log.warn("计数投影对账失败: {}", e.getMessage());
        }
    }

    private void reconcileAll() {
        try {
            int updated = jdbcTemplate.update("""
                    INSERT INTO inspire_metric
                      (inspire_id, view_count, like_count, collect_count, comment_count, share_count, heat)
                    SELECT i.id,
                           COALESCE(i.view_count, 0),
                           COALESCE(l.cnt, 0),
                           COALESCE(c.cnt, 0),
                           COALESCE(m.cnt, 0),
                           COALESCE(i.share_count, 0),
                           COALESCE(i.heat, 0)
                    FROM inspire_main i
                    LEFT JOIN (
                        SELECT inspire_id, COUNT(*) cnt FROM user_like GROUP BY inspire_id
                    ) l ON l.inspire_id = i.id
                    LEFT JOIN (
                        SELECT inspire_id, COUNT(*) cnt FROM collect GROUP BY inspire_id
                    ) c ON c.inspire_id = i.id
                    LEFT JOIN (
                        SELECT inspire_id, COUNT(*) cnt
                        FROM inspire_comment WHERE deleted = 0 GROUP BY inspire_id
                    ) m ON m.inspire_id = i.id
                    WHERE i.deleted = 0
                    ON DUPLICATE KEY UPDATE
                      view_count = VALUES(view_count),
                      like_count = VALUES(like_count),
                      collect_count = VALUES(collect_count),
                      comment_count = VALUES(comment_count),
                      share_count = VALUES(share_count),
                      heat = VALUES(heat)
                    """);
            log.info("计数投影首次全量对账完成: {}", updated);
        } catch (Exception e) {
            initialDone.set(false);
            throw e;
        }
    }

    private void reconcileRecent() {
        int updated = jdbcTemplate.update("""
                INSERT INTO inspire_metric
                  (inspire_id, view_count, like_count, collect_count, comment_count, share_count, heat)
                SELECT i.id, COALESCE(i.view_count, 0),
                       (SELECT COUNT(*) FROM user_like l WHERE l.inspire_id = i.id),
                       (SELECT COUNT(*) FROM collect c WHERE c.inspire_id = i.id),
                       (SELECT COUNT(*) FROM inspire_comment m
                         WHERE m.inspire_id = i.id AND m.deleted = 0),
                       COALESCE(i.share_count, 0), COALESCE(i.heat, 0)
                FROM inspire_main i
                WHERE i.deleted = 0
                  AND (i.update_time >= ?
                       OR NOT EXISTS (SELECT 1 FROM inspire_metric x WHERE x.inspire_id = i.id))
                ON DUPLICATE KEY UPDATE
                  view_count = VALUES(view_count),
                  like_count = VALUES(like_count),
                  collect_count = VALUES(collect_count),
                  comment_count = VALUES(comment_count),
                  share_count = VALUES(share_count),
                  heat = VALUES(heat)
                """, LocalDateTime.now().minusMinutes(15));
        log.debug("计数投影增量对账完成: {}", updated);
    }
}
