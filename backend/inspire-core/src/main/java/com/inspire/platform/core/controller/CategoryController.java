package com.inspire.platform.core.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.entity.Category;
import com.inspire.platform.core.entity.WordCloud;
import com.inspire.platform.core.mapper.CategoryMapper;
import com.inspire.platform.core.mapper.WordCloudMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台公开的分类与词云接口（网关白名单：/api/inspire/public/**）。
 * 数据全部来自后台可维护的 sys_category / sys_word_cloud。
 */
@Tag(name = "分类与词云", description = "前台分类树、AI探索词云")
@RestController
@RequestMapping("/inspire/public")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryMapper categoryMapper;
    private final WordCloudMapper wordCloudMapper;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "分类树", description = "返回两级分类，一级分类带该分类下的灵感数量")
    @GetMapping("/categories")
    @Cacheable(value = "categories", key = "'tree'")
    public Result<List<Map<String, Object>>> categories(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=600");
        List<Category> all = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder));

        Map<String, Integer> tagCount = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT tag, COUNT(*) AS c FROM inspire_main "
                            + "WHERE deleted = 0 AND status = 1 GROUP BY tag");
            for (Map<String, Object> row : rows) {
                Object tag = row.get("tag");
                Object count = row.get("c");
                if (tag != null && count != null) {
                    tagCount.put(String.valueOf(tag), ((Number) count).intValue());
                }
            }
        } catch (Exception ignored) {
            // 统计失败不影响分类展示
        }

        Map<Long, Map<String, Object>> parents = new LinkedHashMap<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                Map<String, Object> node = new LinkedHashMap<>();
                // 雪花 ID 超出 JS 安全整数范围，统一按字符串返回
                node.put("id", String.valueOf(c.getId()));
                node.put("name", c.getName());
                node.put("icon", c.getIcon());
                node.put("count", tagCount.getOrDefault(c.getName(), 0));
                node.put("children", new ArrayList<Map<String, Object>>());
                parents.put(c.getId(), node);
            }
        }
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) continue;
            Map<String, Object> parent = parents.get(c.getParentId());
            if (parent == null) continue;
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("id", String.valueOf(c.getId()));
            child.put("name", c.getName());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
            children.add(child);
            parent.put("count", ((Number) parent.get("count")).intValue()
                    + tagCount.getOrDefault(c.getName(), 0));
        }
        return Result.success(new ArrayList<>(parents.values()));
    }

    @Operation(summary = "AI探索词云", description = "返回启用的词云词条，按权重与排序返回")
    @GetMapping("/word-cloud")
    @Cacheable(value = "wordCloud", key = "'enabled'")
    public Result<List<WordCloud>> wordCloud(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=600");
        return Result.success(wordCloudMapper.selectList(
                Wrappers.<WordCloud>lambdaQuery()
                        .eq(WordCloud::getStatus, 1)
                        .orderByAsc(WordCloud::getSortOrder)));
    }
}
