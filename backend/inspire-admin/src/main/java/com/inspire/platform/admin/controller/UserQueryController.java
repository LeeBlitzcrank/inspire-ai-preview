package com.inspire.platform.admin.controller;
import com.inspire.platform.admin.entity.AdminUserRow;
import com.inspire.platform.admin.service.AdminUserQueryService;
import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@Tag(name = "用户查询", description = "用户搜索、详情查看")
@RestController @RequestMapping("/admin/user") @RequiredArgsConstructor
public class UserQueryController {
    private final AdminUserQueryService userQueryService;

    @Operation(summary = "搜索用户", description = "按用户名/邮箱/昵称搜索")
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> r = new HashMap<>();
        r.put("list", userQueryService.search(keyword, page, size));
        r.put("total", userQueryService.total());
        return Result.success(r);
    }

    @Operation(summary = "用户详情", description = "查看用户基本信息")
    @GetMapping("/{id}")
    public Result<AdminUserRow> detail(@PathVariable Long id) {
        return Result.success(userQueryService.detail(id));
    }
}
