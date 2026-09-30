/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldModels.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public final class WorldModels {

    private WorldModels() {
    }

    public record WorldSeedView(
            String id,
            String sourceTitle,
            String sourceAuthor,
            String sourceText,
            String title,
            String background,
            List<String> rules,
            List<CharacterView> characters,
            String question,
            String coverImage,
            long branches,
            List<WorldLineView> lines
    ) {
    }

    public record WorldLineView(
            String id,
            String seedId,
            String title,
            String variable,
            String environment,
            String characterState,
            String question,
            long branchCount,
            List<WorldBranchView> branches
    ) {
    }

    public record WorldBranchView(
            String id,
            String seedId,
            String lineId,
            String title,
            String ownerUserId,
            String ownerName,
            boolean defaultBranch,
            int visibility,
            long chapterCount,
            Map<String, Long> voteCounts,
            WorldChapterSummary latestChapter
    ) {
    }

    public record WorldChapterSummary(
            String id,
            int chapterNo,
            String title,
            String choiceKey,
            String excerpt,
            String createTime
    ) {
    }

    public record WorldChapterView(
            String id,
            String branchId,
            int chapterNo,
            String choiceKey,
            String title,
            String content,
            String createTime
    ) {
    }

    public record WorldChapterPage(
            List<WorldChapterView> items,
            Integer nextBeforeChapterNo,
            boolean hasMore
    ) {
    }

    public record WorldTaskView(
            String id,
            String taskType,
            String status,
            int progress,
            String seedId,
            String lineId,
            String branchId,
            JsonNode result,
            String errorMessage,
            String createTime,
            String updateTime
    ) {
    }

    public record CharacterView(String name, String description) {
    }

    public record GenerateSeedRequest(
            String sourceTitle,
            String sourceAuthor,
            String sourceText,
            String guidance
    ) {
    }

    public record GenerateChapterRequest(
            long lineId,
            Long branchId,
            String choiceKey,
            String choiceText
    ) {
    }

    public record VoteRequest(
            long branchId,
            long lineId,
            String choiceKey
    ) {
    }

    public record GeneratedChapter(
            String branchId,
            String chapterId,
            int chapterNo,
            String title,
            String content,
            String nextQuestion
    ) {
    }
}
