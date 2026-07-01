package com.inspire.platform.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;
import com.inspire.platform.core.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "灵感评论")
@RestController
@RequestMapping("/inspire")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "评论列表", description = "按灵感ID分页查询评论")
    @GetMapping("/{id}/comments")
    public Result<Page<CommentVO>> list(@PathVariable Long id,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return Result.success(commentService.listByInspireId(id, page, size));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{id}/comment")
    public Result<Void> create(@PathVariable("id") Long inspireId,
                                @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
                                @Valid @RequestBody CommentCreateRequest request) {
        request.setInspireId(inspireId);
        commentService.create(userId, request);
        return Result.success("评论成功", null);
    }

    @Operation(summary = "删除评论", description = "仅限本人删除")
    @DeleteMapping("/{id}/comment/{commentId}")
    public Result<Void> delete(@PathVariable Long id,
                                @PathVariable Long commentId,
                                @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        commentService.deleteById(commentId, userId);
        return Result.success("删除成功", null);
    }
}
