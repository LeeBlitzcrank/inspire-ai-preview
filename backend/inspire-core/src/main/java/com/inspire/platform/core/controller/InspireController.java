package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.InspireMain;
import com.inspire.platform.core.service.InspireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "灵感核心", description = "灵感列表、详情、创建、收藏、点赞")
@RestController
@RequestMapping("/inspire")
@RequiredArgsConstructor
public class InspireController {

    private final InspireService inspireService;

    @Operation(summary = "公开灵感列表", description = "分页+分类筛选，支持按时间/热度排序")
    @GetMapping("/public/list")
    public Result<List<InspireVO>> listPublic(InspirePageQuery query,
            @RequestHeader(value = "X-Inspire-UserId", required = false) Long loginUserId) {
        return Result.success(inspireService.listPublic(query, loginUserId));
    }

    @Operation(summary = "灵感详情", description = "返回完整信息，含正文、收藏/点赞状态，自动增加浏览量")
    @GetMapping("/public/{id}")
    public Result<InspireVO> detail(@PathVariable Long id,
            @RequestHeader(value = "X-Inspire-UserId", required = false) Long loginUserId) {
        return Result.success(inspireService.getDetail(id, loginUserId));
    }

    @Operation(summary = "我的发布", description = "当前用户已公开发布的灵感列表")
    @GetMapping("/my")
    public Result<List<InspireVO>> myPublished(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        return Result.success(inspireService.listMyPublished(userId));
    }

    @Operation(summary = "我的草稿", description = "当前用户未发布的草稿列表")
    @GetMapping("/my/drafts")
    public Result<List<InspireVO>> myDrafts(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        return Result.success(inspireService.listMyDrafts(userId));
    }

    @Operation(summary = "我的收藏", description = "当前用户收藏的灵感列表")
    @GetMapping("/my/collects")
    public Result<List<InspireVO>> myCollects(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        return Result.success(inspireService.listMyCollects(userId));
    }

    @Operation(summary = "创建灵感", description = "status=0保存草稿，status=1直接发布")
    @PostMapping
    public Result<InspireMain> create(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody InspireCreateRequest req) {
        return Result.success("创建成功", inspireService.create(req, userId));
    }

    @Operation(summary = "修改灵感", description = "只传需要修改的字段即可")
    @PutMapping("/{id}")
    public Result<InspireMain> update(@PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestBody InspireUpdateRequest req) {
        return Result.success("修改成功", inspireService.update(id, req, userId));
    }

    @Operation(summary = "删除灵感", description = "逻辑删除，仅限本人")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.deleteById(id, userId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "收藏灵感", description = "不可重复收藏，分表 user_id%10 路由")
    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.collect(userId, inspireId);
        return Result.success("收藏成功", null);
    }

    @Operation(summary = "取消收藏", description = "取消后收藏数-1")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.uncollect(userId, inspireId);
        return Result.success("取消收藏", null);
    }

    @Operation(summary = "点赞灵感", description = "不可重复点赞，分表 inspire_id%10 路由")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.like(userId, inspireId);
        return Result.success("点赞成功", null);
    }

    @Operation(summary = "取消点赞", description = "取消后点赞数-1")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.unlike(userId, inspireId);
        return Result.success("取消点赞", null);
    }
}
