package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.world.WorldAiGenerator.ChapterDraft;
import com.inspire.platform.ai.world.WorldAiGenerator.SeedDraft;
import com.inspire.platform.ai.world.WorldModels.GenerateChapterRequest;
import com.inspire.platform.ai.world.WorldModels.GenerateSeedRequest;
import com.inspire.platform.ai.world.WorldModels.GeneratedChapter;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorldGenerationPersistence {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public long createSeed(GenerateSeedRequest request, SeedDraft draft) {
        long seedId = nextId();
        jdbcTemplate.update("""
                INSERT INTO world_seed
                (id, source_title, source_author, source_text, title, background,
                 rules_json, characters_json, question, cover_image, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '', 1)
                """,
                seedId,
                request.sourceTitle(),
                Objects.toString(request.sourceAuthor(), ""),
                request.sourceText(),
                draft.title(),
                draft.background(),
                writeJson(draft.rules()),
                writeJson(draft.characters()),
                draft.question()
        );
        java.util.List<WorldAiGenerator.LineDraft> lines = draft.lines().isEmpty()
                ? java.util.List.of(new WorldAiGenerator.LineDraft(
                        "原线",
                        "不改变任何条件",
                        "故事起点",
                        "人物处于初始状态",
                        draft.question()
                ))
                : draft.lines();
        for (WorldAiGenerator.LineDraft line : lines) {
            insertDefaultBranch(seedId, line);
        }
        return seedId;
    }

    private void insertDefaultBranch(long seedId, WorldAiGenerator.LineDraft line) {
        long lineId = nextId();
        jdbcTemplate.update("""
                INSERT INTO world_line
                (id, seed_id, title, variable, environment, character_state, question, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """,
                lineId,
                seedId,
                line.title(),
                line.variable(),
                line.environment(),
                line.characterState(),
                line.question()
        );
        jdbcTemplate.update("""
                INSERT INTO world_branch
                (id, seed_id, line_id, owner_user_id, parent_branch_id, title,
                 branch_type, visibility, status)
                VALUES (?, ?, ?, NULL, NULL, ?, 'CANON', 1, 1)
                """,
                lineId,
                seedId,
                lineId,
                "公共原线 · " + line.title()
        );
        jdbcTemplate.update("""
                INSERT INTO world_branch_metric(branch_id) VALUES (?)
                """, lineId);
    }

    @Transactional(readOnly = true)
    public ChapterContext loadChapterContext(GenerateChapterRequest request, long userId) {
        ChapterContext base = jdbcTemplate.query("""
                SELECT l.id AS line_id, l.seed_id, l.title AS line_title, l.variable,
                       l.environment, l.character_state, s.source_text, s.background
                FROM world_line l
                JOIN world_seed s ON s.id = l.seed_id
                WHERE l.id = ? AND l.status = 1 AND s.status = 1
                """, rs -> {
            if (!rs.next()) throw new BusinessException(404, "世界线不存在");
            return new ChapterContext(
                    rs.getLong("seed_id"),
                    rs.getLong("line_id"),
                    rs.getString("line_title"),
                    rs.getString("variable"),
                    rs.getString("environment"),
                    rs.getString("character_state"),
                    rs.getString("source_text"),
                    rs.getString("background"),
                    null,
                    null,
                    ""
            );
        }, request.lineId());

        if (request.branchId() == null) return base;
        BranchRef branch = jdbcTemplate.query("""
                SELECT id, owner_user_id, line_id
                FROM world_branch
                WHERE id = ? AND status = 1 AND visibility = 1
                """, rs -> rs.next()
                ? new BranchRef(rs.getLong("id"), nullableLong(rs, "owner_user_id"), rs.getLong("line_id"))
                : null, request.branchId());
        if (branch == null || branch.lineId() != request.lineId()) {
            throw new BusinessException(404, "世界线分支不存在");
        }
        Long appendBranchId = branch.ownerUserId() != null && branch.ownerUserId() == userId
                ? branch.id()
                : null;
        String latest = Objects.toString(jdbcTemplate.query(
                "SELECT content FROM world_chapter WHERE branch_id = ? ORDER BY chapter_no DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : "",
                branch.id()
        ), "");
        return new ChapterContext(
                base.seedId(),
                base.lineId(),
                base.lineTitle(),
                base.variable(),
                base.environment(),
                base.characterState(),
                base.sourceText(),
                base.background(),
                appendBranchId,
                branch.id(),
                latest
        );
    }

    @Transactional
    public GeneratedChapter saveChapter(long taskId,
                                        long userId,
                                        GenerateChapterRequest request,
                                        ChapterContext context,
                                        ChapterDraft draft) {
        long branchId = context.appendBranchId() == null
                ? createUserBranch(taskId, userId, request, context)
                : lockUserBranch(context.appendBranchId(), userId);
        Integer chapterNo = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(chapter_no), 0) + 1
                FROM world_chapter
                WHERE branch_id = ?
                """, Integer.class, branchId);
        long chapterId = nextId();
        jdbcTemplate.update("""
                INSERT INTO world_chapter
                (id, line_id, branch_id, chapter_no, choice_key, title, content)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                chapterId,
                context.lineId(),
                branchId,
                chapterNo == null ? 1 : chapterNo,
                Objects.toString(request.choiceKey(), ""),
                draft.title(),
                draft.content()
        );
        jdbcTemplate.update("""
                INSERT INTO world_branch_metric(branch_id, chapter_count)
                VALUES (?, 1)
                ON DUPLICATE KEY UPDATE chapter_count = chapter_count + 1
                """, branchId);
        return new GeneratedChapter(
                Long.toString(branchId),
                Long.toString(chapterId),
                chapterNo == null ? 1 : chapterNo,
                draft.title(),
                draft.content(),
                draft.nextQuestion()
        );
    }

    private long createUserBranch(long taskId,
                                  long userId,
                                  GenerateChapterRequest request,
                                  ChapterContext context) {
        long branchId = taskId;
        jdbcTemplate.update("""
                INSERT INTO world_branch
                (id, seed_id, line_id, owner_user_id, parent_branch_id, title,
                 branch_type, visibility, status)
                VALUES (?, ?, ?, ?, ?, ?, 'USER', 1, 1)
                """,
                branchId,
                context.seedId(),
                context.lineId(),
                userId,
                context.parentBranchId(),
                "我的世界线 · " + context.lineTitle()
        );
        return branchId;
    }

    private long lockUserBranch(long branchId, long userId) {
        Long locked = jdbcTemplate.query("""
                SELECT id
                FROM world_branch
                WHERE id = ? AND owner_user_id = ? AND status = 1
                FOR UPDATE
                """, rs -> rs.next() ? rs.getLong(1) : null, branchId, userId);
        if (locked == null) throw new BusinessException(403, "不能续写他人的世界线分支");
        return locked;
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static long nextId() {
        return WorldIds.next();
    }

    private record BranchRef(long id, Long ownerUserId, long lineId) {
    }

    public record ChapterContext(
            long seedId,
            long lineId,
            String lineTitle,
            String variable,
            String environment,
            String characterState,
            String sourceText,
            String background,
            Long appendBranchId,
            Long parentBranchId,
            String latestContent
    ) {
    }
}
