/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldSeedService.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.world.WorldModels.*;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.validation.InputValidation;
import com.inspire.platform.common.validation.ValidationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSeedService {

    private static final long BOOTSTRAP_SEED_ID = 100000000009900001L;
    private static final long BOOTSTRAP_LINE_ID_BASE = 100000000009910000L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final WorldCacheService cacheService;
    private final WorldTaskService taskService;
    private final WorldGenerationWorker generationWorker;

    private volatile boolean bootstrapChecked;

    public List<WorldSeedView> listPublic() {
        ensureBootstrap();
        List<WorldSeedView> cached = cacheService.getList("public:seeds", WorldSeedView.class);
        if (cached != null) return cached;
        List<WorldSeedView> result = jdbcTemplate.query("""
                SELECT s.id, s.source_title, s.source_author, s.source_text, s.title,
                       s.background, s.rules_json, s.characters_json, s.question,
                       s.cover_image,
                       (SELECT COUNT(*) FROM world_line l
                        WHERE l.seed_id = s.id AND l.status = 1) AS branch_count
                FROM world_seed s
                WHERE s.status = 1
                ORDER BY s.update_time DESC, s.id DESC
                LIMIT 50
                """, (rs, rowNum) -> readSeed(rs, false));
        cacheService.put("public:seeds", result, 60);
        return result;
    }

    public WorldSeedView getPublic(long seedId) {
        ensureBootstrap();
        WorldSeedView cached = cacheService.get("public:seed:" + seedId, WorldSeedView.class);
        if (cached != null) return cached;
        WorldSeedView seed = jdbcTemplate.query("""
                SELECT s.id, s.source_title, s.source_author, s.source_text, s.title,
                       s.background, s.rules_json, s.characters_json, s.question,
                       s.cover_image,
                       (SELECT COUNT(*) FROM world_line l
                        WHERE l.seed_id = s.id AND l.status = 1) AS branch_count
                FROM world_seed s
                WHERE s.id = ? AND s.status = 1
                """, rs -> rs.next() ? readSeed(rs, false) : null, seedId);
        if (seed == null) throw new BusinessException(404, "世界种子不存在");
        seed = new WorldSeedView(
                seed.id(),
                seed.sourceTitle(),
                seed.sourceAuthor(),
                seed.sourceText(),
                seed.title(),
                seed.background(),
                seed.rules(),
                seed.characters(),
                seed.question(),
                seed.coverImage(),
                seed.branches(),
                loadLines(seedId)
        );
        cacheService.put("public:seed:" + seedId, seed, 180);
        return seed;
    }

    public WorldChapterPage listChapters(long branchId, Integer beforeChapterNo, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        int before = beforeChapterNo == null ? Integer.MAX_VALUE : Math.max(beforeChapterNo, 1);
        String cacheKey = "branch:chapters:" + branchId + ":" + before + ":" + pageSize;
        WorldChapterPage cached = cacheService.get(cacheKey, WorldChapterPage.class);
        if (cached != null) return cached;

        Integer visible = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM world_branch
                WHERE id = ? AND status = 1 AND visibility = 1
                """, Integer.class, branchId);
        if (visible == null || visible == 0) {
            throw new BusinessException(404, "世界线分支不存在");
        }
        List<WorldChapterView> rows = jdbcTemplate.query("""
                SELECT id, branch_id, chapter_no, choice_key, title, content, create_time
                FROM world_chapter
                WHERE branch_id = ? AND chapter_no < ?
                ORDER BY chapter_no DESC
                LIMIT ?
                """, (rs, rowNum) -> mapChapter(rs), branchId, before, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, pageSize));
        Integer nextBefore = hasMore && !rows.isEmpty()
                ? rows.get(rows.size() - 1).chapterNo()
                : null;
        WorldChapterPage page = new WorldChapterPage(rows, nextBefore, hasMore);
        cacheService.put(cacheKey, page, 60);
        return page;
    }

    public WorldTaskView submitSeedTask(GenerateSeedRequest request, long userId, String idempotencyKey) {
        if (userId <= 0) throw new BusinessException(401, "请先登录");
        GenerateSeedRequest normalizedRequest = new GenerateSeedRequest(
                InputValidation.normalizeRequiredText(
                        request.sourceTitle(), "原文名称", ValidationConstants.WORLD_SOURCE_TITLE_MAX),
                InputValidation.normalizeOptionalText(
                        request.sourceAuthor(), "作者", ValidationConstants.WORLD_SOURCE_AUTHOR_MAX),
                InputValidation.normalizeRequiredText(
                        request.sourceText(), "原文内容", ValidationConstants.WORLD_SOURCE_TEXT_MAX),
                InputValidation.normalizeOptionalText(
                        request.guidance(), "创作方向", ValidationConstants.WORLD_GUIDANCE_MAX)
        );
        WorldTaskView task = taskService.createSeedTask(userId, normalizedRequest, idempotencyKey);
        if (WorldTaskService.STATUS_PENDING.equals(task.status())) {
            generationWorker.submit(Long.parseLong(task.id()));
        }
        return task;
    }

    public WorldTaskView submitChapterTask(GenerateChapterRequest request, long userId, String idempotencyKey) {
        if (userId <= 0) throw new BusinessException(401, "请先登录");
        InputValidation.requirePositive(request.lineId(), "世界线");
        if (request.branchId() != null) {
            InputValidation.requirePositive(request.branchId(), "分支");
        }
        GenerateChapterRequest normalizedRequest = new GenerateChapterRequest(
                request.lineId(),
                request.branchId(),
                InputValidation.normalizeRequiredText(
                        request.choiceKey(), "选择项", ValidationConstants.WORLD_CHOICE_KEY_MAX),
                InputValidation.normalizeRequiredText(
                        request.choiceText(), "选择内容", ValidationConstants.WORLD_CHOICE_TEXT_MAX)
        );
        WorldTaskView task = taskService.createChapterTask(userId, normalizedRequest, idempotencyKey);
        if (WorldTaskService.STATUS_PENDING.equals(task.status())) {
            generationWorker.submit(Long.parseLong(task.id()));
        }
        return task;
    }

    public WorldTaskView getTask(long taskId, long userId) {
        return taskService.getRequired(taskId, userId);
    }

    @Transactional
    public void vote(VoteRequest request, long userId) {
        if (userId <= 0) throw new BusinessException(401, "请先登录");
        long branchId = request.branchId() > 0 ? request.branchId() : request.lineId();
        InputValidation.requirePositive(request.lineId(), "世界线");
        InputValidation.requirePositive(branchId, "分支");
        String choiceKey = InputValidation.normalizeRequiredText(
                request.choiceKey(), "选择项", ValidationConstants.WORLD_CHOICE_KEY_MAX);
        BranchRef branch = jdbcTemplate.query("""
                SELECT id, line_id, owner_user_id
                FROM world_branch
                WHERE id = ? AND status = 1 AND visibility = 1
                FOR UPDATE
                """, rs -> rs.next()
                ? new BranchRef(rs.getLong("id"), rs.getLong("line_id"), nullableLong(rs, "owner_user_id"))
                : null, branchId);
        if (branch == null) throw new BusinessException(404, "世界线分支不存在");

        String oldChoice = jdbcTemplate.query("""
                SELECT choice_key
                FROM world_vote
                WHERE branch_id = ? AND user_id = ?
                FOR UPDATE
                """, rs -> rs.next() ? rs.getString(1) : null, branchId, userId);
        String newChoice = normalizeChoice(choiceKey);
        if (Objects.equals(oldChoice, newChoice)) return;

        if (oldChoice == null) {
            jdbcTemplate.update("""
                    INSERT INTO world_vote(id, line_id, branch_id, user_id, choice_key)
                    VALUES (?, ?, ?, ?, ?)
                    """, nextId(), branch.lineId(), branchId, userId, newChoice);
            updateMetric(branchId, newChoice, 1, 1);
        } else {
            jdbcTemplate.update("""
                    UPDATE world_vote
                    SET branch_id = ?, line_id = ?, choice_key = ?, create_time = NOW()
                    WHERE branch_id = ? AND user_id = ?
                    """, branchId, branch.lineId(), newChoice, branchId, userId);
            updateMetric(branchId, oldChoice, -1, 0);
            updateMetric(branchId, newChoice, 1, 0);
        }
        cacheService.invalidateAll();
    }

    private WorldSeedView readSeed(ResultSet rs, boolean withLines) throws SQLException {
        long seedId = rs.getLong("id");
        return new WorldSeedView(
                Long.toString(seedId),
                rs.getString("source_title"),
                rs.getString("source_author"),
                rs.getString("source_text"),
                rs.getString("title"),
                rs.getString("background"),
                readJsonList(rs.getString("rules_json"), String.class),
                readJsonList(rs.getString("characters_json"), CharacterView.class),
                rs.getString("question"),
                rs.getString("cover_image"),
                rs.getLong("branch_count"),
                withLines ? loadLines(seedId) : List.of()
        );
    }

    private List<WorldLineView> loadLines(long seedId) {
        List<LineRow> lineRows = jdbcTemplate.query("""
                SELECT id, seed_id, title, variable, environment, character_state, question
                FROM world_line
                WHERE seed_id = ? AND status = 1
                ORDER BY id ASC
                """, (rs, rowNum) -> new LineRow(
                rs.getLong("id"),
                rs.getLong("seed_id"),
                rs.getString("title"),
                rs.getString("variable"),
                rs.getString("environment"),
                rs.getString("character_state"),
                rs.getString("question")
        ), seedId);
        if (lineRows.isEmpty()) return List.of();

        String placeholders = lineRows.stream().map(row -> "?").collect(Collectors.joining(","));
        Object[] lineIds = lineRows.stream().map(LineRow::id).toArray();
        Map<Long, List<WorldBranchView>> branchesByLine = new HashMap<>();
        jdbcTemplate.query("""
                SELECT b.id, b.seed_id, b.line_id, b.title, b.owner_user_id,
                       COALESCE(u.nickname, '公共原线') AS owner_name,
                       b.branch_type, b.visibility,
                       COALESCE(m.chapter_count, 0) AS chapter_count,
                       COALESCE(m.vote_continue, 0) AS vote_continue,
                       COALESCE(m.vote_change, 0) AS vote_change,
                       COALESCE(m.vote_question, 0) AS vote_question,
                       COALESCE(m.vote_custom, 0) AS vote_custom
                FROM world_branch b
                LEFT JOIN `user` u ON u.id = b.owner_user_id
                LEFT JOIN world_branch_metric m ON m.branch_id = b.id
                WHERE b.line_id IN (%s) AND b.status = 1 AND b.visibility = 1
                ORDER BY CASE WHEN b.branch_type = 'CANON' THEN 0 ELSE 1 END,
                         b.update_time DESC
                """.formatted(placeholders), rs -> {
            long lineId = rs.getLong("line_id");
            Map<String, Long> votes = new HashMap<>();
            votes.put("continue", rs.getLong("vote_continue"));
            votes.put("change", rs.getLong("vote_change"));
            votes.put("question", rs.getLong("vote_question"));
            votes.put("custom", rs.getLong("vote_custom"));
            branchesByLine.computeIfAbsent(lineId, ignored -> new ArrayList<>())
                    .add(new WorldBranchView(
                            Long.toString(rs.getLong("id")),
                            Long.toString(rs.getLong("seed_id")),
                            Long.toString(lineId),
                            rs.getString("title"),
                            nullableLongString(rs, "owner_user_id"),
                            rs.getString("owner_name"),
                            "CANON".equals(rs.getString("branch_type")),
                            rs.getInt("visibility"),
                            rs.getLong("chapter_count"),
                            votes,
                            null
                    ));
        }, lineIds);

        Map<Long, WorldChapterSummary> latestByBranch = loadLatestChapters(lineRows);
        return lineRows.stream().map(line -> {
            List<WorldBranchView> branches = branchesByLine
                    .getOrDefault(line.id(), List.of())
                    .stream()
                    .map(branch -> new WorldBranchView(
                            branch.id(),
                            branch.seedId(),
                            branch.lineId(),
                            branch.title(),
                            branch.ownerUserId(),
                            branch.ownerName(),
                            branch.defaultBranch(),
                            branch.visibility(),
                            branch.chapterCount(),
                            branch.voteCounts(),
                            latestByBranch.get(Long.parseLong(branch.id()))
                    ))
                    .toList();
            return new WorldLineView(
                    Long.toString(line.id()),
                    Long.toString(line.seedId()),
                    line.title(),
                    line.variable(),
                    line.environment(),
                    line.characterState(),
                    line.question(),
                    branches.size(),
                    branches
            );
        }).toList();
    }

    private Map<Long, WorldChapterSummary> loadLatestChapters(List<LineRow> lines) {
        Map<Long, WorldChapterSummary> result = new HashMap<>();
        String placeholders = lines.stream().map(row -> "?").collect(Collectors.joining(","));
        Object[] lineIds = lines.stream().map(LineRow::id).toArray();
        jdbcTemplate.query("""
                SELECT id, branch_id, chapter_no, choice_key, title, excerpt, create_time
                FROM (
                  SELECT c.id, c.branch_id, c.chapter_no, c.choice_key, c.title,
                         LEFT(c.content, 140) AS excerpt, c.create_time,
                         ROW_NUMBER() OVER (PARTITION BY c.branch_id ORDER BY c.chapter_no DESC) AS rn
                  FROM world_chapter c
                  JOIN world_branch b ON b.id = c.branch_id
                  WHERE b.line_id IN (%s) AND b.status = 1 AND b.visibility = 1
                ) t
                WHERE rn = 1
                """.formatted(placeholders), rs -> {
            result.put(rs.getLong("branch_id"), new WorldChapterSummary(
                    Long.toString(rs.getLong("id")),
                    rs.getInt("chapter_no"),
                    rs.getString("title"),
                    rs.getString("choice_key"),
                    rs.getString("excerpt"),
                    timeString(rs.getTimestamp("create_time"))
            ));
        }, lineIds);
        return result;
    }

    private WorldChapterView mapChapter(ResultSet rs) throws SQLException {
        return new WorldChapterView(
                Long.toString(rs.getLong("id")),
                Long.toString(rs.getLong("branch_id")),
                rs.getInt("chapter_no"),
                rs.getString("choice_key"),
                rs.getString("title"),
                rs.getString("content"),
                timeString(rs.getTimestamp("create_time"))
        );
    }

    private void updateMetric(long branchId, String choice, long delta, long totalDelta) {
        String column = switch (choice) {
            case "continue" -> "vote_continue";
            case "change" -> "vote_change";
            case "question" -> "vote_question";
            default -> "vote_custom";
        };
        jdbcTemplate.update("""
                INSERT INTO world_branch_metric(branch_id, %s, vote_total)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    %s = GREATEST(%s + VALUES(%s), 0),
                    vote_total = GREATEST(vote_total + VALUES(vote_total), 0)
                """.formatted(column, column, column, column, column),
                branchId,
                Math.max(delta, 0),
                Math.max(totalDelta, 0)
        );
        if (delta < 0 || totalDelta < 0) {
            jdbcTemplate.update("""
                    UPDATE world_branch_metric
                    SET %s = GREATEST(%s - 1, 0),
                        vote_total = GREATEST(vote_total - ?, 0)
                    WHERE branch_id = ?
                    """.formatted(column, column),
                    Math.max(-totalDelta, 0),
                    branchId
            );
        }
    }

    private String normalizeChoice(String choice) {
        return switch (choice) {
            case "continue", "change", "question" -> choice;
            default -> "custom";
        };
    }

    private String nullableLongString(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.toString(value);
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String timeString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private <T> List<T> readJsonList(String value, Class<T> type) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return objectMapper.readValue(
                    value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, type)
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private synchronized void ensureBootstrap() {
        if (bootstrapChecked) return;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM world_seed", Integer.class);
        if (count != null && count > 0) {
            bootstrapChecked = true;
            return;
        }
        int inserted = jdbcTemplate.update("""
                INSERT IGNORE INTO world_seed
                (id, source_title, source_author, source_text, title, background,
                 rules_json, characters_json, question, cover_image, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '', 1)
                """,
                BOOTSTRAP_SEED_ID,
                "西游记·第一回",
                "吴承恩",
                "海外有一国土，名曰傲来国。国近大海，海中有一座名山，唤为花果山。那座山正当顶上，有一块仙石。内育仙胞，一日迸裂，产一石卵，因见风，化作一个石猴。众猴拍手称扬道：那一个有本事的，钻进去寻个源头出来，不伤身体者，我等即拜他为王。石猴瞑目蹲身，将身一纵，径跳入瀑布泉中。",
                "水帘洞没有成为王座",
                """
                花果山临海而立，山脚是终年不散的潮声，山顶则常常先于别处看见日出。石猴从仙石中生出时没有名字，也没有族谱。他学会行走、攀爬和观察猴群，却始终觉得自己比这片山林多出一层无法解释的清醒。水帘洞出现后，猴群第一次有了共同目标：谁能穿过瀑布，谁就有资格成为王。石猴跳进洞口，看见洞天、石桥和暗河流向大海。他很快意识到，这里并不是一座宫殿，而是一条连接花果山与外部世界的秘密通道。
                石猴后来成为美猴王，但王位并没有给他答案。他发现猴群会因为他一句话改变路线，会因为他一个表情停止争吵，也会把偶然的决定当成永远的法律。那种权力既让他兴奋，也让他不安。他开始记录每一次分食、每一次迁徙和每一个离开花果山的人，担心自己有一天忘掉名字，其他人却仍会把他当成唯一正确的方向。
                水帘洞深处还有一道从未打开的石门。石门上没有锁，只有一行会随潮气变化的符号。每逢月圆，符号会短暂亮起，像是在提示某种未来。石猴曾问过最年长的猴子，对方只记得祖父说过：门不是为猴王准备的，而是为某个准备离开王位的人准备的。
                天空偶尔会出现不属于花果山的云。它们移动得太快，边缘带着金色，像有谁从很高的地方观察这座山。石猴知道那可能是天庭，也可能只是自己的想象。他没有把这件事告诉猴群，因为一旦说出口，花果山就会从自然的家园变成被注视的棋盘。
                与此同时，花果山的水位正在缓慢下降。暗河仍在地下流动，瀑布却一年比一年细。最先发现变化的是负责取水的母猴，随后是常在岸边睡觉的老猴。石猴没有立即公布消息，他想先找到源头。因为他明白，如果瀑布消失，水帘洞将不再是屏障，花果山的王位也会失去最重要的仪式。
                他开始在石壁上记录每一次水位变化，把日期、天气和潮声都画成只有自己能看懂的符号。猴群以为这是新王留下的法令，便照着符号调整取水路线。记录本来是石猴给自己准备的记忆，却在不知不觉中变成了整座山的行动指南。
                故事开始于一个看似普通的清晨：石猴站在洞口，听见体内传来不属于瀑布的声音。那声音叫出了他的未来名字，却没有告诉他这个名字会带来荣耀还是代价。猴群在身后等待命令，天庭在云上等待机会，石门在黑暗里等待被选择的人。所有世界线都将从这一刻分岔。
                """,
                writeJson(List.of(
                        "水帘洞并不是王座，而是一条通往外部世界的通道",
                        "花果山的瀑布水量正在持续下降",
                        "洞底石门只会为准备离开王位的人打开"
                )),
                writeJson(List.of(
                        new CharacterView("石猴", "刚成为美猴王，还不知道名字意味着责任还是束缚"),
                        new CharacterView("老猴", "记得花果山旧日传说，但不愿说出全部真相"),
                        new CharacterView("天门观察者", "来自天庭的隐秘注视者，从未真正露面")
                )),
                "如果瀑布消失，花果山还需要一个王吗？"
        );
        if (inserted == 0) {
            bootstrapChecked = true;
            return;
        }
        insertPresetLine(BOOTSTRAP_SEED_ID, BOOTSTRAP_LINE_ID_BASE + 1,
                "原线：石猴进入水帘洞", "不改变任何条件", "花果山·水帘洞", "石猴成为美猴王", "成为王以后，他是否还能保留离开的自由？");
        insertPresetLine(BOOTSTRAP_SEED_ID, BOOTSTRAP_LINE_ID_BASE + 2,
                "停在洞口", "石猴没有跳进瀑布", "花果山·瀑布前", "石猴失去称王的机会", "没有王的花果山会更自由吗？");
        insertPresetLine(BOOTSTRAP_SEED_ID, BOOTSTRAP_LINE_ID_BASE + 3,
                "镜中未来", "石猴提前看到五百年后的取经路", "水帘洞·石镜前", "他知道未来但不知道因果", "知道结局后，还会选择成为齐天大圣吗？");
        insertPresetLine(BOOTSTRAP_SEED_ID, BOOTSTRAP_LINE_ID_BASE + 4,
                "天兵提前", "玉帝在石猴闹龙宫前派人下界", "花果山·金光初现", "太白金星提前到来", "招安是机会还是束缚？");
        insertPresetLine(BOOTSTRAP_SEED_ID, BOOTSTRAP_LINE_ID_BASE + 5,
                "不争王位", "猴群共同治理水帘洞", "水帘洞·猴群议事", "花果山形成共同体", "没有王，秩序会更稳定还是更混乱？");
        bootstrapChecked = true;
    }

    private void insertPresetLine(long seedId, long lineId, String title, String variable,
                                  String environment, String characterState, String question) {
        jdbcTemplate.update("""
                INSERT INTO world_line
                (id, seed_id, title, variable, environment, character_state, question, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """,
                lineId, seedId, title, variable, environment, characterState, question
        );
        jdbcTemplate.update("""
                INSERT INTO world_branch
                (id, seed_id, line_id, owner_user_id, parent_branch_id, title,
                 branch_type, visibility, status)
                VALUES (?, ?, ?, NULL, NULL, ?, 'CANON', 1, 1)
                """,
                lineId, seedId, lineId, "公共原线 · " + title
        );
        jdbcTemplate.update("""
                INSERT INTO world_branch_metric(branch_id) VALUES (?)
                """, lineId);
    }

    private static long nextId() {
        return WorldIds.next();
    }

    private record LineRow(
            long id,
            long seedId,
            String title,
            String variable,
            String environment,
            String characterState,
            String question
    ) {
    }

    private record BranchRef(long id, long lineId, Long ownerUserId) {
    }
}
