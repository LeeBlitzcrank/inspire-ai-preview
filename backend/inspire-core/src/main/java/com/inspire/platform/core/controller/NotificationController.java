package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Tag(name = "消息通知")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的通知列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.list(userId, page, size));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("count", notificationService.unreadCount(userId));
        return Result.success(data);
    }

    @Operation(summary = "标记已读", description = "notificationId传null则标记全部已读")
    @PutMapping("/read")
    public Result<Void> markRead(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestParam(required = false) Long notificationId) {
        notificationService.markRead(userId, notificationId);
        return Result.success("已标记已读", null);
    }
}
