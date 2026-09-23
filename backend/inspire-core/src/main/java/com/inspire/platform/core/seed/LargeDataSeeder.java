package com.inspire.platform.core.seed;

import com.inspire.platform.common.util.TitleUtil;
import com.inspire.platform.core.service.es.EsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 大数据压测数据生成器。
 *
 * 开启方式：
 *   INSPIRE_DEMO_SEED=true
 *   INSPIRE_DEMO_SCALE=large
 *
 * 该填充器在基础种子之后运行，使用独立 ID 段和批量插入，
 * 不会影响默认的小数据种子模式。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inspire.demo.scale", havingValue = "large")
public class LargeDataSeeder implements ApplicationRunner {

    private static final int USER_COUNT = 10_000;
    private static final int INSPIRE_COUNT = 12_000;
    private static final int FOLDER_COUNT = 10_000;
    private static final int FOLLOW_COUNT = 20_000;
    private static final int CONVERSATION_COUNT = 2_000;
    private static final int MESSAGE_COUNT = 20_000;
    private static final int NOTIFICATION_COUNT = 20_000;
    private static final int AI_HISTORY_COUNT = 12_000;
    private static final int COMMENT_LIKE_COUNT = 20_000;

    private static final long USER_ID = 400_000_000_000_000_000L;
    private static final long INSPIRE_ID = 410_000_000_000_000_000L;
    private static final long FOLDER_ID = 420_000_000_000_000_000L;
    private static final long COLLECT_ID = 430_000_000_000_000_000L;
    private static final long LIKE_ID = 440_000_000_000_000_000L;
    private static final long COMMENT_ID = 450_000_000_000_000_000L;
    private static final long COMMENT_LIKE_ID = 460_000_000_000_000_000L;
    private static final long FOLLOW_ID = 470_000_000_000_000_000L;
    private static final long CONVERSATION_ID = 480_000_000_000_000_000L;
    private static final long MEMBER_ID = 490_000_000_000_000_000L;
    private static final long MESSAGE_ID = 500_000_000_000_000_000L;
    private static final long NOTIFICATION_ID = 510_000_000_000_000_000L;
    private static final long AI_HISTORY_ID = 520_000_000_000_000_000L;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final String[] CITIES = {
            "北京", "上海", "杭州", "成都", "广州", "深圳", "西安", "重庆", "南京", "厦门", "长沙", "苏州"
    };
    private static final String[] AVATARS = {"🌿", "☕", "📷", "🏕", "🧶", "📚", "🎬", "🍜"};
    private static final String[] TITLE_PATTERNS = {
            "%s｜真实体验", "%s实用思路", "%s避坑指南", "低成本%s计划",
            "%s清单", "%s入门记录", "%s复盘笔记"
    };
    private static final String[] CHAT = {
            "这个灵感很有启发", "我周末也想去试试", "照片拍得真好看", "谢谢分享，收藏了",
            "这个方法很实用", "你最近更新得好勤", "有空一起交流", "已经转给朋友看了"
    };

    private final JdbcTemplate jdbcTemplate;
    private final EsSyncService esSyncService;
    private final Random rnd = new Random(20260923L);

    @Value("${inspire.image.cdn-domain:https://img.20sherry.com}")
    private String cdnDomain;

    private record CategorySeed(long categoryId, String categoryName,
                                long subCategoryId, String subCategoryName) {}

    private record InspireRef(long id, String title, LocalDateTime createTime) {}

    @Override
    public void run(ApplicationArguments args) {
        try {
            seed();
        } catch (Exception e) {
            log.error("[LargeSeeder] 大数据生成失败", e);
        }
    }

    private void seed() {
        Integer perfUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE username LIKE 'perf%' AND deleted = 0",
                Integer.class);
        if (perfUsers != null && perfUsers >= USER_COUNT) {
            log.info("[LargeSeeder] 已存在 {} 个压测用户，跳过生成", perfUsers);
            return;
        }
        if (perfUsers != null && perfUsers > 0) {
            log.warn("[LargeSeeder] 检测到 {} 个压测用户，疑似上次生成中断；"
                    + "为避免脏数据，本次跳过，请重置后重试", perfUsers);
            return;
        }

        long started = System.currentTimeMillis();
        log.info("[LargeSeeder] 开始生成大数据：用户={}，灵感={}，关注={}，会话={}，消息={}，通知={}，AI历史={}",
                USER_COUNT, INSPIRE_COUNT, FOLLOW_COUNT, CONVERSATION_COUNT,
                MESSAGE_COUNT, NOTIFICATION_COUNT, AI_HISTORY_COUNT);

        ensureUsers();
        ensureFolders();
        List<CategorySeed> categories = loadCategories();
        List<InspireRef> inspires = generateInspires(categories);
        generateLikesAndCollects();
        generateComments();
        generateFollows();
        generateConversations();
        generateNotifications(inspires);
        generateAiHistory();

        esSyncService.batchSync();
        log.info("[LargeSeeder] 大数据生成完成，耗时 {} ms", System.currentTimeMillis() - started);
    }

    private void ensureUsers() {
        String password = PASSWORD_ENCODER.encode("112233");
        List<Object[]> batch = new ArrayList<>(1000);
        for (int i = 0; i < USER_COUNT; i++) {
            batch.add(new Object[]{
                    userId(i),
                    String.format("perf%05d", i + 1),
                    password,
                    String.format("perf%05d@demo.local", i + 1),
                    AVATARS[i % AVATARS.length],
                    String.format("压测用户%05d", i + 1),
                    CITIES[i % CITIES.length]
            });
            if (batch.size() >= 1000) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO `user`(id,username,password,email,avatar,nickname,city,deleted) "
                                + "VALUES(?,?,?,?,?,?,?,0)",
                        batch);
                batch.clear();
            }
        }
        flush(
                "INSERT INTO `user`(id,username,password,email,avatar,nickname,city,deleted) "
                        + "VALUES(?,?,?,?,?,?,?,0)",
                batch);
        log.info("[LargeSeeder] 压测用户生成完成: {}", USER_COUNT);
    }

    private void ensureFolders() {
        List<Object[]> batch = new ArrayList<>(1000);
        for (int i = 0; i < FOLDER_COUNT; i++) {
            int userIndex = i % USER_COUNT;
            batch.add(new Object[]{
                    folderId(i),
                    userId(userIndex),
                    String.format("压测收藏夹%05d", i / USER_COUNT + 1),
                    "📂",
                    i / USER_COUNT,
                    Timestamp.valueOf(now().minusDays(rnd.nextInt(365)))
            });
            if (batch.size() >= 1000) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO collect_folder(id,user_id,name,icon,sort_order,create_time) "
                                + "VALUES(?,?,?,?,?,?)",
                        batch);
                batch.clear();
            }
        }
        flush(
                "INSERT INTO collect_folder(id,user_id,name,icon,sort_order,create_time) "
                        + "VALUES(?,?,?,?,?,?)",
                batch);
        log.info("[LargeSeeder] 收藏夹生成完成: {}", FOLDER_COUNT);
    }

    private List<CategorySeed> loadCategories() {
        return jdbcTemplate.query(
                "SELECT child.id AS sub_id, child.name AS sub_name, "
                        + "parent.id AS category_id, parent.name AS category_name "
                        + "FROM sys_category child "
                        + "JOIN sys_category parent ON parent.id = child.parent_id "
                        + "WHERE child.parent_id <> 0 AND child.status = 1 AND child.deleted = 0 "
                        + "AND parent.status = 1 AND parent.deleted = 0",
                (rs, rowNum) -> new CategorySeed(
                        rs.getLong("category_id"), rs.getString("category_name"),
                        rs.getLong("sub_id"), rs.getString("sub_name")));
    }

    private List<InspireRef> generateInspires(List<CategorySeed> categories) {
        List<InspireRef> refs = new ArrayList<>(INSPIRE_COUNT);
        List<Object[]> mainBatch = new ArrayList<>(500);
        List<Object[]> contentBatch = new ArrayList<>(500);
        for (int i = 0; i < INSPIRE_COUNT; i++) {
            CategorySeed category = categories.get(rnd.nextInt(categories.size()));
            int ownerIndex = rnd.nextInt(USER_COUNT);
            String title = buildTitle(category.subCategoryName());
            String image = demoImage(i);
            String images = "[\"" + image + "\",\"" + demoImage(i + 7) + "\"]";
            LocalDateTime createTime = now().minusDays(rnd.nextInt(730)).minusMinutes(rnd.nextInt(1440));
            int likes = 4;
            int collects = 4;
            long views = 100 + rnd.nextInt(100_000);
            long heat = views + likes * 10L + collects * 20L;

            mainBatch.add(new Object[]{
                    inspireId(i), title, image, images, category.categoryName(),
                    category.categoryId(), category.subCategoryId(), userId(ownerIndex),
                    1, views, likes, collects, heat, 3 + rnd.nextInt(18),
                    CITIES[rnd.nextInt(CITIES.length)], Timestamp.valueOf(createTime)
            });
            contentBatch.add(new Object[]{
                    inspireId(i), buildContent(category.categoryName(), category.subCategoryName()),
                    Timestamp.valueOf(createTime), Timestamp.valueOf(createTime)
            });
            refs.add(new InspireRef(inspireId(i), title, createTime));

            if (mainBatch.size() >= 500) {
                flushInspireBatches(mainBatch, contentBatch);
            }
        }
        flushInspireBatches(mainBatch, contentBatch);
        log.info("[LargeSeeder] 灵感生成完成: {}", INSPIRE_COUNT);
        return refs;
    }

    private void flushInspireBatches(List<Object[]> mainBatch, List<Object[]> contentBatch) {
        if (mainBatch.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "INSERT INTO inspire_main(id,title,img,images,tag,category_id,sub_category_id,user_id,"
                        + "status,view_count,like_count,collect_count,heat,share_count,publish_city,create_time,deleted) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                mainBatch);
        jdbcTemplate.batchUpdate(
                "INSERT INTO inspire_content(inspire_id,content,create_time,update_time) VALUES(?,?,?,?)",
                contentBatch);
        mainBatch.clear();
        contentBatch.clear();
    }

    private void generateLikesAndCollects() {
        List<Object[]> likes = new ArrayList<>();
        List<Object[]> collects = new ArrayList<>();
        long likeSeq = 0, collectSeq = 0;
        for (int i = 0; i < INSPIRE_COUNT; i++) {
            LocalDateTime base = now().minusDays(rnd.nextInt(365));
            for (int k = 0; k < 4; k++) {
                int actor = Math.floorMod(i * 37 + k * 997, USER_COUNT);
                likes.add(new Object[]{
                        LIKE_ID + likeSeq++, userId(actor), inspireId(i),
                        Timestamp.valueOf(base.plusMinutes(rnd.nextInt(2000)))
                });

                int collector = Math.floorMod(i * 53 + k * 883 + 7, USER_COUNT);
                collects.add(new Object[]{
                        COLLECT_ID + collectSeq++, userId(collector), inspireId(i),
                        folderId(collector % FOLDER_COUNT),
                        Timestamp.valueOf(base.plusMinutes(rnd.nextInt(2000)))
                });
            }
            if ((i + 1) % 2000 == 0) {
                flush("INSERT INTO user_like(id,user_id,inspire_id,create_time) VALUES(?,?,?,?)", likes);
                flush("INSERT INTO collect(id,user_id,inspire_id,folder_id,create_time) VALUES(?,?,?,?,?)", collects);
            }
        }
        flush("INSERT INTO user_like(id,user_id,inspire_id,create_time) VALUES(?,?,?,?)", likes);
        flush("INSERT INTO collect(id,user_id,inspire_id,folder_id,create_time) VALUES(?,?,?,?,?)", collects);
        log.info("[LargeSeeder] 点赞明细生成完成: {}，收藏明细生成完成: {}", likeSeq, collectSeq);
    }

    private void generateComments() {
        List<Object[]> comments = new ArrayList<>();
        List<Object[]> commentLikes = new ArrayList<>();
        Map<Long, Long> commentInspire = new HashMap<>();
        List<Long> commentIds = new ArrayList<>();
        long commentSeq = 0;
        for (int i = 0; i < INSPIRE_COUNT; i++) {
            int count = 2 + rnd.nextInt(2);
            long rootId = COMMENT_ID + commentSeq++;
            int rootUserIndex = rnd.nextInt(USER_COUNT);
            long rootUser = userId(rootUserIndex);
            LocalDateTime base = now().minusDays(rnd.nextInt(60)).minusMinutes(rnd.nextInt(1440));
            comments.add(new Object[]{
                    rootId, inspireId(i), rootUser, nickOf(rootUserIndex), AVATARS[(int) (rootUserIndex % AVATARS.length)],
                    0L, rootId, 0L, "", "压测主评论 " + i, rnd.nextInt(80),
                    Timestamp.valueOf(base), Timestamp.valueOf(base)
            });
            commentIds.add(rootId);
            commentInspire.put(rootId, inspireId(i));

            for (int k = 1; k < count; k++) {
                long replyId = COMMENT_ID + commentSeq++;
                int replyUserIndex = rnd.nextInt(USER_COUNT);
                LocalDateTime replyTime = base.plusMinutes(1 + rnd.nextInt(180));
                comments.add(new Object[]{
                        replyId, inspireId(i), userId(replyUserIndex), nickOf(replyUserIndex),
                        AVATARS[replyUserIndex % AVATARS.length], rootId, rootId, rootUser, nickOf(rootUserIndex),
                        "压测回复 " + i + "-" + k, rnd.nextInt(20),
                        Timestamp.valueOf(replyTime), Timestamp.valueOf(replyTime)
                });
                commentIds.add(replyId);
                commentInspire.put(replyId, inspireId(i));
            }
            if ((i + 1) % 2000 == 0) {
                flushComments(comments);
            }
        }
        flushComments(comments);

        int likeCount = Math.min(COMMENT_LIKE_COUNT, commentIds.size());
        for (int i = 0; i < likeCount; i++) {
            int actorIndex = Math.floorMod(i * 17, USER_COUNT);
            commentLikes.add(new Object[]{
                    COMMENT_LIKE_ID + i, userId(actorIndex), commentIds.get(i),
                    commentInspire.getOrDefault(commentIds.get(i), inspireId(0)),
                    Timestamp.valueOf(now().minusDays(rnd.nextInt(90)))
            });
            if ((i + 1) % 2000 == 0) {
                flush("INSERT INTO comment_like(id,user_id,comment_id,inspire_id,create_time) VALUES(?,?,?,?,?)", commentLikes);
            }
        }
        flush("INSERT INTO comment_like(id,user_id,comment_id,inspire_id,create_time) VALUES(?,?,?,?,?)", commentLikes);
        log.info("[LargeSeeder] 评论生成完成: {}，评论点赞生成完成: {}", commentSeq, likeCount);
    }

    private void flushComments(List<Object[]> comments) {
        if (comments.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "INSERT INTO inspire_comment"
                        + "(id,inspire_id,user_id,author_nickname,avatar,parent_id,root_id,"
                        + "reply_user_id,reply_nickname,content,like_count,create_time,update_time,deleted) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                comments);
        comments.clear();
    }

    private void generateFollows() {
        Set<Long> keys = new HashSet<>(FOLLOW_COUNT * 2);
        List<Object[]> batch = new ArrayList<>(1000);
        long seq = 0;
        while (seq < FOLLOW_COUNT) {
            int follower = rnd.nextInt(USER_COUNT);
            int followee = rnd.nextInt(USER_COUNT);
            if (follower == followee) continue;
            long key = (long) follower * USER_COUNT + followee;
            if (!keys.add(key)) continue;
            batch.add(new Object[]{
                    FOLLOW_ID + seq++, userId(follower), userId(followee),
                    Timestamp.valueOf(now().minusDays(rnd.nextInt(365)))
            });
            if (batch.size() >= 1000) {
                flush("INSERT INTO user_follow(id,follower_id,followee_id,create_time) VALUES(?,?,?,?)", batch);
            }
        }
        flush("INSERT INTO user_follow(id,follower_id,followee_id,create_time) VALUES(?,?,?,?)", batch);
        log.info("[LargeSeeder] 关注关系生成完成: {}", FOLLOW_COUNT);
    }

    private void generateConversations() {
        Set<Long> pairs = new HashSet<>(CONVERSATION_COUNT * 2);
        List<Object[]> conversations = new ArrayList<>(500);
        List<Object[]> members = new ArrayList<>(1000);
        List<Object[]> messages = new ArrayList<>(2000);
        long conversationSeq = 0, memberSeq = 0, messageSeq = 0;
        while (conversationSeq < CONVERSATION_COUNT) {
            int left = rnd.nextInt(USER_COUNT);
            int right = rnd.nextInt(USER_COUNT);
            if (left == right) continue;
            int a = Math.min(left, right);
            int b = Math.max(left, right);
            long key = (long) a * USER_COUNT + b;
            if (!pairs.add(key)) continue;

            long conversationId = CONVERSATION_ID + conversationSeq++;
            LocalDateTime cursor = now().minusDays(rnd.nextInt(180)).withHour(9).withMinute(0).withSecond(0).withNano(0);
            String last = "";
            int lastFrom = a;
            for (int i = 0; i < 10; i++) {
                int from = i % 2 == 0 ? a : b;
                int to = from == a ? b : a;
                last = CHAT[rnd.nextInt(CHAT.length)];
                lastFrom = from;
                cursor = cursor.plusMinutes(3 + rnd.nextInt(120));
                messages.add(new Object[]{
                        MESSAGE_ID + messageSeq++, conversationId,
                        userId(from), userId(to), last, Timestamp.valueOf(cursor)
                });
            }
            conversations.add(new Object[]{
                    conversationId, userId(a), userId(b), MESSAGE_ID + messageSeq - 1,
                    last, Timestamp.valueOf(cursor),
                    Timestamp.valueOf(cursor.minusDays(1)), Timestamp.valueOf(cursor)
            });
            members.add(new Object[]{
                    MEMBER_ID + memberSeq++, conversationId, userId(a),
                    lastFrom == a ? 0 : 1, 0L, 0L, 0,
                    Timestamp.valueOf(cursor), Timestamp.valueOf(cursor.minusDays(1)), Timestamp.valueOf(cursor)
            });
            members.add(new Object[]{
                    MEMBER_ID + memberSeq++, conversationId, userId(b),
                    lastFrom == b ? 0 : 1, 0L, 0L, 0,
                    Timestamp.valueOf(cursor), Timestamp.valueOf(cursor.minusDays(1)), Timestamp.valueOf(cursor)
            });

            if (conversations.size() >= 500) {
                flushConversationBatches(conversations, members, messages);
            }
        }
        flushConversationBatches(conversations, members, messages);
        log.info("[LargeSeeder] 会话生成完成: {}，消息生成完成: {}", conversationSeq, messageSeq);
    }

    private void flushConversationBatches(List<Object[]> conversations,
                                          List<Object[]> members,
                                          List<Object[]> messages) {
        if (conversations.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "INSERT INTO message_conversation(id,user1_id,user2_id,last_message_id,last_content,last_time,"
                        + "create_time,update_time) VALUES(?,?,?,?,?,?,?,?)",
                conversations);
        jdbcTemplate.batchUpdate(
                "INSERT INTO conversation_member(id,conversation_id,user_id,unread_count,last_read_message_id,"
                        + "deleted_before_message_id,deleted,last_time,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?)",
                members);
        jdbcTemplate.batchUpdate(
                "INSERT INTO message(id,conversation_id,from_user_id,to_user_id,content,create_time) "
                        + "VALUES(?,?,?,?,?,?)",
                messages);
        conversations.clear();
        members.clear();
        messages.clear();
    }

    private void generateNotifications(List<InspireRef> inspires) {
        List<Object[]> batch = new ArrayList<>(1000);
        String[] types = {"like", "collect", "comment"};
        for (int i = 0; i < NOTIFICATION_COUNT; i++) {
            InspireRef target = inspires.get(rnd.nextInt(inspires.size()));
            int ownerIndex = rnd.nextInt(USER_COUNT);
            int actorIndex = rnd.nextInt(USER_COUNT);
            if (actorIndex == ownerIndex) actorIndex = (actorIndex + 1) % USER_COUNT;
            String type = types[rnd.nextInt(types.length)];
            String content = "like".equals(type) ? "点赞了你的灵感"
                    : "collect".equals(type) ? "收藏了你的灵感" : "评论了你的灵感";
            batch.add(new Object[]{
                    NOTIFICATION_ID + i, userId(ownerIndex), type, userId(actorIndex),
                    nickOf(actorIndex), content, target.id(), target.title(),
                    rnd.nextInt(3) == 0 ? 0 : 1, Timestamp.valueOf(now().minusDays(rnd.nextInt(180)))
            });
            if (batch.size() >= 1000) {
                flush(
                        "INSERT INTO user_notification(id,user_id,type,actor_id,actor_name,content,"
                                + "target_id,target_title,is_read,deleted,create_time) VALUES(?,?,?,?,?,?,?,?,?,0,?)",
                        batch);
            }
        }
        flush(
                "INSERT INTO user_notification(id,user_id,type,actor_id,actor_name,content,"
                        + "target_id,target_title,is_read,deleted,create_time) VALUES(?,?,?,?,?,?,?,?,?,0,?)",
                batch);
        log.info("[LargeSeeder] 通知生成完成: {}", NOTIFICATION_COUNT);
    }

    private void generateAiHistory() {
        List<Object[]> batch = new ArrayList<>(500);
        for (int i = 0; i < AI_HISTORY_COUNT; i++) {
            int userIndex = i % USER_COUNT;
            String keyword = "压测灵感" + (i % 300);
            String title = "压测历史标题" + (i % 100);
            String result = "{\"summary\":\"" + keyword + "\",\"content\":{\"title\":\""
                    + title + "\",\"text\":\"用于列表和缓存读取的压测历史数据\"}}";
            LocalDateTime createTime = now().minusDays(rnd.nextInt(365));
            batch.add(new Object[]{
                    AI_HISTORY_ID + i, userId(userIndex), keyword, "stress",
                    "stress:" + keyword + ":" + i, result, i % 5, title,
                    i % 3 == 0 ? "selected" : "generated",
                    Timestamp.valueOf(createTime), Timestamp.valueOf(createTime)
            });
            if (batch.size() >= 500) {
                flush(
                        "INSERT INTO user_ai_history(id,user_id,keyword,path,cache_key,result_json,"
                                + "selected_index,selected_title,status,create_time,update_time,deleted) "
                                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",
                        batch);
            }
        }
        flush(
                "INSERT INTO user_ai_history(id,user_id,keyword,path,cache_key,result_json,"
                        + "selected_index,selected_title,status,create_time,update_time,deleted) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",
                batch);
        log.info("[LargeSeeder] AI 历史生成完成: {}", AI_HISTORY_COUNT);
    }

    private void flush(String sql, List<Object[]> batch) {
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
            batch.clear();
        }
    }

    private long userId(int index) {
        return USER_ID + index;
    }

    private long inspireId(int index) {
        return INSPIRE_ID + index;
    }

    private long folderId(int index) {
        return FOLDER_ID + index;
    }

    private String nickOf(int index) {
        return String.format("压测用户%05d", index + 1);
    }

    private String demoImage(long seed) {
        return cdnDomain.replaceAll("/+$", "") + "/upload/demo/" + Math.floorMod(seed, 60) + ".jpg";
    }

    private String buildTitle(String topic) {
        String pattern = TITLE_PATTERNS[rnd.nextInt(TITLE_PATTERNS.length)];
        return TitleUtil.truncate(String.format(pattern, topic));
    }

    private String buildContent(String tag, String topic) {
        return "最近围绕「" + topic + "」做了一轮压测记录。"
                + "这套" + tag + "思路会覆盖真实列表、详情、搜索、收藏、评论和分页场景。\n\n"
                + "1. 先准备可复用的数据关系。\n"
                + "2. 再验证接口和缓存命中情况。\n"
                + "3. 最后观察长列表滚动和查询耗时。\n\n"
                + "内容会保持章节结构，确保详情页、搜索摘要和 AI 历史读取都有稳定的数据形态。";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
