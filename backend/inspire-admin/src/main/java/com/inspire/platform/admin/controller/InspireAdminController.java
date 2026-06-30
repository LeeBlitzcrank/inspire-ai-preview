package com.inspire.platform.admin.controller;
import com.inspire.platform.admin.service.AdminInspireService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@Tag(name = "灵感管理", description = "全量灵感列表、下架/上架操作")
@RestController
@RequestMapping("/admin/inspire")
@RequiredArgsConstructor
public class InspireAdminController {
    private final AdminInspireService adminInspireService;
    @Operation(summary = "灵感列表", description = "分页+搜索，支持关键字/分类/状态筛选")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminInspireService.list(keyword, tag, status, page, size));
    }
    @Operation(summary = "下架灵感", description = "将灵感移入黑名单，不再参与推荐与检索")
    @PutMapping("/{id}/block")
    public Result<Void> block(@PathVariable Long id) {
        adminInspireService.block(id);
        return Result.success("已下架", null);
    }
    @Operation(summary = "上架灵感", description = "将灵感从黑名单恢复")
    @PutMapping("/{id}/unblock")
    public Result<Void> unblock(@PathVariable Long id) {
        adminInspireService.unblock(id);
        return Result.success("已上架", null);
    }
}
