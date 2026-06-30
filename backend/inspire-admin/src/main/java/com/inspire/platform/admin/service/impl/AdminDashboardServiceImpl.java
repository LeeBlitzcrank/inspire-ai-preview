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

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setInspireTotal(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE deleted=0 AND status=1", Long.class));
        vo.setUserTotal(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE deleted=0", Long.class));
        vo.setAiCallCount(0); // 暂未对接AI调用统计

        // 分类热度
        Map<String, Integer> tagHeat = new HashMap<>();
        jdbcTemplate.query("SELECT tag, COUNT(*) as cnt FROM inspire_main WHERE deleted=0 AND status=1 GROUP BY tag",
                rs -> { tagHeat.put(rs.getString("tag"), rs.getInt("cnt")); });
        vo.setTagHeat(tagHeat);

        // 城市分布（只取有值的）
        Map<String, Integer> cityStats = new HashMap<>();
        jdbcTemplate.query("SELECT publish_city, COUNT(*) as cnt FROM inspire_main WHERE deleted=0 AND status=1 AND publish_city != '' GROUP BY publish_city",
                rs -> { cityStats.put(rs.getString("publish_city"), rs.getInt("cnt")); });
        vo.setCityStats(cityStats);
        return vo;
    }
}
