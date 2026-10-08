/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/mapper/RecommendPushMapper.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：数据访问接口，负责数据库读写映射
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.admin.entity.RecommendPush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendPushMapper extends BaseMapper<RecommendPush> {

    @Select("""
            SELECT p.*, i.title, i.img
            FROM recommend_push p
            LEFT JOIN inspire_main i ON i.id = p.inspire_id
            ORDER BY p.update_time DESC, p.id DESC
            LIMIT 200
            """)
    List<RecommendPush> selectWithInspire();

    @Select("""
            SELECT p.id,
                   p.inspire_id,
                   i.title,
                   p.status,
                   SUM(e.event_type = 'IMPRESSION') AS impressions,
                   SUM(e.event_type = 'CLICK') AS clicks,
                   SUM(e.event_type = 'COLLECT') AS collects,
                   SUM(e.event_type = 'SKIP') AS skips,
                   ROUND(AVG(CASE WHEN e.event_type = 'DWELL' THEN e.duration_ms END), 0) AS avg_dwell_ms
            FROM recommend_push p
            LEFT JOIN inspire_main i ON i.id = p.inspire_id
            LEFT JOIN recommend_event e ON e.push_id = p.id
            GROUP BY p.id, p.inspire_id, i.title, p.status
            ORDER BY p.update_time DESC, p.id DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectPushMetrics();
}
