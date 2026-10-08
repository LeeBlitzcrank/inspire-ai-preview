/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldModels.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

import static com.inspire.platform.common.validation.ValidationConstants.*;

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
            @NotBlank(message = "原文名称不能为空")
            @Size(max = WORLD_SOURCE_TITLE_MAX, message = "原文名称不能超过120个字符")
            String sourceTitle,
            @Size(max = WORLD_SOURCE_AUTHOR_MAX, message = "作者名称不能超过80个字符")
            String sourceAuthor,
            @NotBlank(message = "原文内容不能为空")
            @Size(max = WORLD_SOURCE_TEXT_MAX, message = "原文内容不能超过6000个字符")
            String sourceText,
            @Size(max = WORLD_GUIDANCE_MAX, message = "创作方向不能超过500个字符")
            String guidance
    ) {
    }

    public record GenerateChapterRequest(
            @Positive(message = "世界线不正确")
            long lineId,
            Long branchId,
            @NotBlank(message = "选择项不能为空")
            @Size(max = WORLD_CHOICE_KEY_MAX, message = "选择项标识过长")
            String choiceKey,
            @NotBlank(message = "选择内容不能为空")
            @Size(max = WORLD_CHOICE_TEXT_MAX, message = "选择内容不能超过160个字符")
            String choiceText
    ) {
    }

    public record VoteRequest(
            long branchId,
            @Positive(message = "世界线不正确")
            long lineId,
            @NotBlank(message = "选择项不能为空")
            @Size(max = WORLD_CHOICE_KEY_MAX, message = "选择项标识过长")
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
