package com.inspire.platform.core.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热度分定时计算
 * 公式: heat = FLOOR(view_count + like_count * 10 + collect_count * 20)
 * 新灵感热度权重更高（时间衰减），这里简化处理直接按互动量计算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatScoreTask {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 15 * 60 * 1000) // 每15分钟
    public void recalculateHeat() {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE inspire_main SET heat = FLOOR(view_count + like_count * 10 + collect_count * 20) " +
                "WHERE deleted = 0 AND status = 1");
            log.info("[热度] 更新完成, 影响 {} 条灵感", updated);
        } catch (Exception e) {
            log.warn("[热度] 计算失败: {}", e.getMessage());
        }
    }
}
