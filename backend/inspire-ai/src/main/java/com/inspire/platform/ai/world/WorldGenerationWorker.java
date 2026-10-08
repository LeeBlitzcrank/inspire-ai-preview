/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldGenerationWorker.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.world.WorldAiGenerator.ChapterDraft;
import com.inspire.platform.ai.world.WorldAiGenerator.SeedDraft;
import com.inspire.platform.ai.world.WorldModels.GenerateChapterRequest;
import com.inspire.platform.ai.world.WorldModels.GenerateSeedRequest;
import com.inspire.platform.ai.world.WorldModels.GeneratedChapter;
import com.inspire.platform.ai.world.WorldTaskService.TaskExecution;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldGenerationWorker {

    private final ObjectMapper objectMapper;
    private final WorldTaskService taskService;
    private final WorldAiGenerator aiGenerator;
    private final WorldGenerationPersistence persistence;
    private final WorldCacheService cacheService;

    @Qualifier("worldGenerationExecutor")
    private final ThreadPoolTaskExecutor executor;

    public void submit(long taskId) {
        executor.execute(() -> execute(taskId));
    }

    public void execute(long taskId) {
        if (!taskService.claim(taskId)) return;
        TaskExecution execution = taskService.loadForExecution(taskId);
        if (execution == null) return;
        try {
            if (WorldTaskService.TYPE_SEED.equals(execution.taskType())) {
                executeSeed(execution);
            } else if (WorldTaskService.TYPE_CHAPTER.equals(execution.taskType())) {
                executeChapter(execution);
            } else {
                throw new BusinessException(400, "不支持的世界生成任务类型");
            }
        } catch (BusinessException e) {
            taskService.fail(taskId, e.getMessage());
            log.warn("世界生成任务失败 taskId={}, code={}, message={}", taskId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            taskService.fail(taskId, "生成失败，请稍后重试");
            log.error("世界生成任务异常 taskId={}", taskId, e);
        }
    }

    @Scheduled(
            initialDelayString = "${inspire.ai.world.recovery-initial-delay-ms:15000}",
            fixedDelayString = "${inspire.ai.world.recovery-delay-ms:30000}"
    )
    public void recoverPendingTasks() {
        try {
            for (Long taskId : taskService.findRecoverableTaskIds()) {
                submit(taskId);
            }
        } catch (Exception e) {
            log.warn("世界生成任务恢复检查暂不可用，将在下一轮重试: {}", e.getMessage());
        }
    }

    private void executeSeed(TaskExecution execution) throws Exception {
        GenerateSeedRequest request = objectMapper.readValue(
                execution.requestJson(),
                GenerateSeedRequest.class
        );
        taskService.updateProgress(execution.taskId(), 15);
        SeedDraft draft = aiGenerator.generateSeed(request);
        taskService.updateProgress(execution.taskId(), 75);
        long seedId = persistence.createSeed(request, draft);
        cacheService.invalidateAll();
        taskService.complete(
                execution.taskId(),
                java.util.Map.of("seedId", Long.toString(seedId)),
                seedId,
                null,
                null
        );
    }

    private void executeChapter(TaskExecution execution) throws Exception {
        GenerateChapterRequest request = objectMapper.readValue(
                execution.requestJson(),
                GenerateChapterRequest.class
        );
        taskService.updateProgress(execution.taskId(), 15);
        WorldGenerationPersistence.ChapterContext context =
                persistence.loadChapterContext(request, execution.userId());
        taskService.updateProgress(execution.taskId(), 35);
        ChapterDraft draft = aiGenerator.generateChapter(
                new WorldAiGenerator.ChapterContext(
                        context.sourceText(),
                        context.background(),
                        context.lineTitle(),
                        context.variable(),
                        context.environment(),
                        context.characterState(),
                        context.latestContent()
                ),
                request.choiceKey(),
                request.choiceText()
        );
        taskService.updateProgress(execution.taskId(), 85);
        GeneratedChapter chapter = persistence.saveChapter(
                execution.taskId(),
                execution.userId(),
                request,
                context,
                draft
        );
        cacheService.invalidateAll();
        taskService.complete(
                execution.taskId(),
                chapter,
                context.seedId(),
                context.lineId(),
                Long.parseLong(chapter.branchId())
        );
    }
}
