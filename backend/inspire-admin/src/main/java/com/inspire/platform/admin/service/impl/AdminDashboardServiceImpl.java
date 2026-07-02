package com.inspire.platform.admin.service.impl;

import com.inspire.platform.admin.dto.DashboardVO;
import com.inspire.platform.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    // 查询所有分表的总和
    private long sumShards(String tablePrefix, String condition) {
        long total = 0;
        for (int i = 0; i < 10; i++) {
            try {
                total += jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tablePrefix + i + " " + condition, Long.class);
            } catch (Exception e) { /* 表不存在则跳过 */ }
        }
        return total;
    }

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        String today = "WHERE DATE(create_time) = CURDATE()";

        // 基础统计
        vo.setInspireTotal(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE deleted=0 AND status=1", Long.class));
        vo.setUserTotal(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE deleted=0", Long.class));
        try {
            vo.setAiCallCount(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_call_log WHERE call_date = CURDATE()", Long.class));
        } catch (Exception e) { vo.setAiCallCount(0); }

        // 今日统计
        vo.setTodayPublish(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE DATE(create_time) = CURDATE()", Long.class));
        vo.setTodayRegister(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE DATE(create_time) = CURDATE()", Long.class));
        vo.setTodayLikes(sumShards("inspire_like_", today));
        vo.setTodayCollects(sumShards("collect_", today));
        vo.setTotalLikes(sumShards("inspire_like_", ""));
        vo.setTotalCollects(sumShards("collect_", ""));

        // DAU: 今日有发布/点赞/收藏/评论的用户数（去重）
        try {
            Long dau = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM ("
                + " SELECT user_id FROM inspire_main WHERE DATE(create_time) = CURDATE()"
                + " UNION SELECT user_id FROM inspire_comment WHERE DATE(create_time) = CURDATE()"
                + ") u", Long.class);
            vo.setDau(dau != null ? dau : 0);
        } catch (Exception e) { vo.setDau(0); }

        // 分类热度
        Map<String, Integer> tagHeat = new HashMap<>();
        jdbcTemplate.query("SELECT tag, COUNT(*) as cnt FROM inspire_main WHERE deleted=0 AND status=1 GROUP BY tag",
                rs -> { tagHeat.put(rs.getString("tag"), rs.getInt("cnt")); });
        vo.setTagHeat(tagHeat);

        // 城市分布
        Map<String, Integer> cityStats = new HashMap<>();
        jdbcTemplate.query("SELECT city, COUNT(*) as cnt FROM user WHERE deleted=0 AND city != '' GROUP BY city",
                rs -> { cityStats.put(rs.getString("city"), rs.getInt("cnt")); });
        vo.setCityStats(cityStats);
        return vo;
    }
}
