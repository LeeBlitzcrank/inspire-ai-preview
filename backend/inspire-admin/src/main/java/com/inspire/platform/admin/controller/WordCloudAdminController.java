/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/controller/WordCloudAdminController.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.entity.WordCloud;
import com.inspire.platform.admin.mapper.WordCloudMapper;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 探索词云管理（后台）：控制前台创建页词云展示哪些词。
 */
@Tag(name = "词云管理", description = "AI探索词云增删改查")
@RestController
@RequestMapping("/admin/word-cloud")
@RequiredArgsConstructor
public class WordCloudAdminController {

    private final WordCloudMapper wordCloudMapper;

    @Operation(summary = "词云列表", description = "返回全部词条（含停用项）")
    @GetMapping("/list")
    public Result<List<WordCloud>> list() {
        return Result.success(wordCloudMapper.selectList(
                Wrappers.<WordCloud>lambdaQuery().orderByAsc(WordCloud::getSortOrder)));
    }

    @Operation(summary = "新增词条")
    @PostMapping
    @CacheEvict(value = "wordCloud", allEntries = true)
    public Result<WordCloud> create(@Valid @RequestBody WordCloud body) {
        if (body.getWord() == null || body.getWord().isBlank()) {
            return Result.error("词条内容不能为空");
        }
        body.setId(null);
        if (body.getWeight() == null) body.setWeight(1);
        if (body.getSortOrder() == null) body.setSortOrder(0);
        if (body.getStatus() == null) body.setStatus(1);
        body.setDeleted(0);
        wordCloudMapper.insert(body);
        return Result.success("新增成功", body);
    }

    @Operation(summary = "更新词条")
    @PutMapping
    @CacheEvict(value = "wordCloud", allEntries = true)
    public Result<Void> update(@Valid @RequestBody WordCloud body) {
        if (body.getId() == null) return Result.error("缺少词条 id");
        wordCloudMapper.updateById(body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除词条", description = "逻辑删除")
    @DeleteMapping("/{id}")
    @CacheEvict(value = "wordCloud", allEntries = true)
    public Result<Void> delete(@PathVariable Long id) {
        wordCloudMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
