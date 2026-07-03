package com.inspire.platform.search.service.impl;

import com.inspire.platform.search.dto.SearchResultVO;
import com.inspire.platform.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("mysqlSearchService")
public class MysqlSearchService implements SearchService {

    private final JdbcTemplate jdbcTemplate;

    public MysqlSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter) {
        int offset = (page - 1) * size;
        String like = "%" + keyword + "%";

        String sql = "SELECT id, title, img, tag, heat, view_count, like_count, collect_count, publish_city, create_time " +
                     "FROM inspire_main WHERE status = 1 AND deleted = 0 " +
                     "AND (title LIKE ? OR tag LIKE ?) " +
                     (tag != null && !tag.isEmpty() ? "AND tag = ? " : "") +
                     "ORDER BY heat DESC LIMIT ? OFFSET ?";

        List<Object> params = new java.util.ArrayList<>();
        params.add(like); params.add(like);
        if (tag != null && !tag.isEmpty()) params.add(tag);
        params.add(size); params.add(offset);

        log.info("MySQL搜索: keyword={}, tag={}, page={}", keyword, tag, page);
        return jdbcTemplate.query(sql, params.toArray(), (rs, row) -> {
            SearchResultVO vo = new SearchResultVO();
            vo.setId(rs.getLong("id"));
            vo.setTitle(rs.getString("title"));
            vo.setImg(rs.getString("img"));
            vo.setTag(rs.getString("tag"));
            vo.setHeat(rs.getInt("heat"));
            vo.setViewCount(rs.getLong("view_count"));
            vo.setLikeCount(rs.getInt("like_count"));
            vo.setCollectCount(rs.getInt("collect_count"));
            vo.setPublishCity(rs.getString("publish_city"));
            vo.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
            vo.setSource("mysql");
            return vo;
        });
    }
}
