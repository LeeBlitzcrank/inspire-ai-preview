package com.inspire.platform.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.entity.Category;
import com.inspire.platform.admin.mapper.CategoryMapper;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类管理（后台）：控制前台「灵感分类」的两级分类展示。
 * 删除为逻辑删除，删除一级分类会连带删除其子分类。
 */
@Tag(name = "分类管理", description = "两级分类增删改查")
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryMapper categoryMapper;

    @Operation(summary = "分类列表", description = "返回两级分类树（含停用项）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Category> all = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSortOrder));

        Map<Long, Map<String, Object>> parents = new LinkedHashMap<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                Map<String, Object> node = new LinkedHashMap<>();
                // 雪花 ID 超过 JS 安全整数范围，必须转成字符串返回，否则前端会丢精度
                node.put("id", String.valueOf(c.getId()));
                node.put("parentId", "0");
                node.put("name", c.getName());
                node.put("icon", c.getIcon());
                node.put("sortOrder", c.getSortOrder());
                node.put("status", c.getStatus());
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
            child.put("parentId", String.valueOf(c.getParentId()));
            child.put("name", c.getName());
            child.put("sortOrder", c.getSortOrder());
            child.put("status", c.getStatus());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
            children.add(child);
        }
        return Result.success(new ArrayList<>(parents.values()));
    }

    @Operation(summary = "新增分类", description = "parentId 为 0 时新增一级分类")
    @PostMapping
    public Result<Category> create(@RequestBody Category body) {
        if (body.getName() == null || body.getName().isBlank()) {
            return Result.error("分类名称不能为空");
        }
        body.setId(null);
        if (body.getParentId() == null) body.setParentId(0L);
        if (body.getSortOrder() == null) body.setSortOrder(0);
        if (body.getStatus() == null) body.setStatus(1);
        body.setDeleted(0);
        categoryMapper.insert(body);
        return Result.success("新增成功", body);
    }

    @Operation(summary = "更新分类", description = "按 id 更新名称、图标、排序、状态")
    @PutMapping
    public Result<Void> update(@RequestBody Category body) {
        if (body.getId() == null) return Result.error("缺少分类 id");
        categoryMapper.updateById(body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除分类", description = "逻辑删除；删除一级分类时连带删除其子分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        categoryMapper.delete(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        return Result.success("删除成功", null);
    }
}
