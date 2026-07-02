package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "用户关注")
@RestController
@RequestMapping("/inspire/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "关注用户")
    @PostMapping("/{userId}")
    public Result<Void> follow(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long myId,
            @PathVariable Long userId) {
        followService.follow(myId, userId);
        return Result.success("关注成功", null);
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{userId}")
    public Result<Void> unfollow(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long myId,
            @PathVariable Long userId) {
        followService.unfollow(myId, userId);
        return Result.success("已取消关注", null);
    }

    @Operation(summary = "我的关注列表")
    @GetMapping("/following")
    public Result<List<Map<String, Object>>> following(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long myId) {
        return Result.success(followService.getFollowing(myId));
    }

    @Operation(summary = "我的粉丝列表")
    @GetMapping("/followers")
    public Result<List<Map<String, Object>>> followers(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long myId) {
        return Result.success(followService.getFollowers(myId));
    }

    @Operation(summary = "关注流灵感", description = "我关注的人发布的灵感，可选指定用户")
    @GetMapping("/feed")
    public Result<List<com.inspire.platform.core.dto.InspireVO>> feed(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long myId,
            @RequestParam(required = false) Long followeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFeed(myId, followeeId, page, size));
    }
}
