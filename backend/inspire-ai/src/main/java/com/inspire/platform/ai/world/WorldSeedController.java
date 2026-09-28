package com.inspire.platform.ai.world;

import com.inspire.platform.ai.world.WorldModels.*;
import com.inspire.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/world")
@RequiredArgsConstructor
public class WorldSeedController {

    private final WorldSeedService worldSeedService;

    @GetMapping("/public/seeds")
    public Result<List<WorldSeedView>> listPublic() {
        return Result.success(worldSeedService.listPublic());
    }

    @GetMapping("/public/seeds/{seedId}")
    public Result<WorldSeedView> getPublic(@PathVariable long seedId) {
        return Result.success(worldSeedService.getPublic(seedId));
    }

    @GetMapping("/public/branches/{branchId}/chapters")
    public Result<WorldChapterPage> chapters(
            @PathVariable long branchId,
            @RequestParam(required = false) Integer beforeChapterNo,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(worldSeedService.listChapters(branchId, beforeChapterNo, size));
    }

    @PostMapping("/generate")
    public Result<WorldTaskView> generate(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody GenerateSeedRequest request) {
        return Result.success("世界生成任务已提交",
                worldSeedService.submitSeedTask(request, userId == null ? 0 : userId, idempotencyKey));
    }

    @PostMapping("/chapter")
    public Result<WorldTaskView> chapter(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody GenerateChapterRequest request) {
        return Result.success("章节生成任务已提交",
                worldSeedService.submitChapterTask(request, userId == null ? 0 : userId, idempotencyKey));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<WorldTaskView> task(
            @PathVariable long taskId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(worldSeedService.getTask(taskId, userId == null ? 0 : userId));
    }

    @PostMapping("/vote")
    public Result<Void> vote(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody VoteRequest request) {
        worldSeedService.vote(request, userId == null ? 0 : userId);
        return Result.success("投票成功", null);
    }
}
