package com.inspire.platform.core.seed;

import com.inspire.platform.core.config.MinioConfig;
import com.inspire.platform.core.config.ShardContext;
import com.inspire.platform.core.entity.*;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.ImageVariantService;
import com.inspire.platform.core.service.es.EsSyncService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * 演示数据生成器（方案 B：走后端，自己保证分表路由与计数一致）
 *
 * 开启方式：inspire.demo.seed=true（或环境变量 INSPIRE_DEMO_SEED=true）
 * 幂等：目标用户已有灵感数据时直接跳过，重复启动不会重复灌。
 *
 * 分表规则必须和 ShardContext 一致，写错会出现「插了数据但页面显示 0」：
 *   collect_N         按 user_id    % 10
 *   user_like_N       按 user_id    % 10
 *   inspire_comment_N 按 inspire_id % 10
 *
 * 热度公式与 HeatScoreTask 保持一致：heat = view + like*10 + collect*20
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inspire.demo.seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final InspireMainMapper mainMapper;
    private final InspireContentMapper contentMapper;
    private final CollectMapper collectMapper;
    private final LikeMapper likeMapper;
    private final InspireCommentMapper commentMapper;
    private final CollectFolderMapper folderMapper;
    private final MessageConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ImageVariantService imageVariantService;
    private final EsSyncService esSyncService;

    @Value("${inspire.image.cdn-domain:https://img.20sherry.com}")
    private String cdnDomain;

    /** 固定 ID 段：当前雪花 ID 约 2.27e17，这里从 1e17 起，绝不会撞号 */
    private static final long ID_BASE = 100_000_000_000_000_000L;
    private static final long USER_ID = ID_BASE + 1L;
    private static final long INSPIRE_ID = ID_BASE + 100_000L;
    private static final long FOLDER_ID = ID_BASE + 200_000L;
    private static final long COLLECT_ID = ID_BASE + 300_000L;
    private static final long LIKE_ID = ID_BASE + 500_000L;
    private static final long COMMENT_ID = ID_BASE + 700_000L;
    private static final long FOLLOW_ID = ID_BASE + 800_000L;
    private static final long CONV_ID = ID_BASE + 900_000L;
    private static final long MEMBER_ID = ID_BASE + 910_000L;
    private static final long MSG_ID = ID_BASE + 950_000L;
    private static final long NOTIFY_ID = ID_BASE + 990_000L;

    /** 评论填充：单条灵感目标评论量 200~300（用于详情页分页演示） */
    private static final int RICH_COMMENT_MIN = 200;
    private static final int RICH_COMMENT_MAX = 300;
    /** 每个演示用户按热度挑选的灵感条数，也填充成 200~300 条评论 */
    private static final int RICH_COMMENT_PER_USER = 5;
    /** 指定重点填充的灵感（当前详情页演示地址所用的灵感） */
    private static final long[] FEATURED_COMMENT_INSPIRES = { INSPIRE_ID + 284L };

    /** 固定随机种子，保证每次生成的数据一致、可复现 */
    private final Random rnd = new Random(20260920L);

    /** 演示账号密码的加密器，必须与 inspire-auth 的校验方式一致（BCrypt） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    /** 全新重置时自动创建的 admin 账号 ID */
    private static final long ADMIN_ID = ID_BASE - 1L;

    /** 演示环境 admin 的默认密码，可用环境变量 INSPIRE_DEMO_ADMIN_PASSWORD 覆盖 */
    @Value("${inspire.demo.admin-password:112233}")
    private String demoAdminPassword;

    private long inspireSeq = 0, folderSeq = 0, collectSeq = 0, likeSeq = 0;
    private long commentSeq = 0, convSeq = 0, msgSeq = 0, memberSeq = 0, notifySeq = 0, followSeq = 0;

    /** 用户名 / 昵称 / 头像 / 城市 / 灵感条数 */
    private static final String[][] NEW_USERS = {
            {"user001", "温暖小兔", "🐰", "杭州", "200"},
            {"user002", "清爽奶茶", "🧁", "成都", "300"},
            {"user003", "文艺胶片", "📷", "上海", "500"}
    };

    private record CategoryChoice(Long categoryId, String categoryName,
                                  Long subCategoryId, String subCategoryName) {}

    /** 从 sys_category 动态加载，种子灵感严格引用后台两级分类。 */
    private final List<CategoryChoice> categoryChoices = new ArrayList<>();

    private static final String[] CITIES = {
            "北京", "上海", "杭州", "成都", "广州", "深圳", "西安", "重庆", "南京", "厦门", "长沙", "苏州"
    };

    /** 每个分类的正文句库（不带句号，拼接时统一补） */
    private static final Map<String, String[]> SENTENCES = Map.of(
            "家居", new String[]{
                    "把不常用的东西收进封闭柜，台面只留每天真正会用到的两三件",
                    "灯光分三层来做：顶灯负责基础照明，落地灯和台灯负责氛围",
                    "墙面适当留白比堆满装饰更耐看，一幅画或一面镜子就够了",
                    "动线尽量保持一条直线，从门口到窗边不要有阻挡",
                    "整体颜色控制在三种以内，深浅搭配比五颜六色更高级"},
            "美食", new String[]{
                    "调味先少后多，出锅前再补一次盐，比一开始下重手稳妥",
                    "食材提前一晚处理好，第二天开火十分钟就能出锅",
                    "热锅冷油、全程中火，是大部分家常菜不出错的底层逻辑",
                    "留一点汤汁拌饭，比收得太干更好吃",
                    "摆盘不用复杂，换个浅色盘子、撒一点葱花就很像样"},
            "旅行", new String[]{
                    "行程只排半天，剩下半天留给临时起意，反而记得更牢",
                    "住处尽量挨着地铁或公交枢纽，省下的时间比省下的钱值钱",
                    "出发前一晚按使用顺序分层装箱，取东西不用翻箱倒柜",
                    "避开整点打卡的热门机位，早一小时或晚一小时人就少一半",
                    "当天的照片当晚就整理好，回家以后基本不会再翻"},
            "摄影", new String[]{
                    "先找光再找景，逆光或侧逆光的层次比顺光耐看得多",
                    "构图上宁可留白多一点，也别把主体卡在正中间",
                    "手机拍夜景时手动把曝光压半档，暗部噪点会明显减少",
                    "同一场景多换两个角度，蹲下或者抬高往往就有新意",
                    "调色只动曝光、对比和色温三处，先克制再谈风格"},
            "穿搭", new String[]{
                    "先定上装再定下装，整体比例比单件好看更重要",
                    "同色系深浅搭配最不容易出错，用材质做区分层次就出来了",
                    "腰线位置决定视觉身高，上衣塞一点进去立刻显利落",
                    "配饰只留一到两件，多了反而显得用力",
                    "衣柜里只保留一年穿过三次以上的单品，其余都可以清掉"},
            "运动", new String[]{
                    "先把频率做起来再加量，每周三次比偶尔一次猛练有效",
                    "跑前动态拉伸、跑后静态拉伸，第二天腿不会那么沉",
                    "配速不用追，能边跑边正常说话的强度就是合适的",
                    "力量训练隔天做，给肌肉留出恢复时间",
                    "把运动排在固定时间，靠惯性比靠意志力靠谱"},
            "文案", new String[]{
                    "先写完整句子再删，删到只剩必要的词，力量就出来了",
                    "具体名词永远比抽象形容词更打动人",
                    "把想说的话先讲给一个具体的人听，语气自然就对了",
                    "结尾留一点没说完，比把道理讲透更耐读",
                    "写完隔一天再读一遍，当时觉得妙的句子自己就会删掉"},
            "电影", new String[]{
                    "经典片隔几年重看一次，看到的其实是被生活改变了的自己",
                    "先看导演的其他作品再看这一部，很多镜头语言就懂了",
                    "把喜欢的片段单独截下来，反复看几遍比整部过一遍收获更大",
                    "记录一句台词就够了，不必写完整影评",
                    "挑片子的时候相信自己的好奇心，别被评分绑架"},
            "生活", new String[]{
                    "把大目标拆成每天二十分钟能完成的小事，坚持反而容易",
                    "固定一件小事当作一天的锚点，比如早起后的一杯水",
                    "每周留半天不安排任何事，用来消化这一周的疲惫",
                    "手机充电放在客厅，卧室只留一本书",
                    "记账不是省钱，是让自己看清钱花在了哪里"},
            "手作", new String[]{
                    "工具比材料重要，先备齐基础工具再囤料",
                    "第一次做别追求成品完美，先把手感练出来",
                    "步骤拆细，每完成一步拍一张照，出问题容易定位",
                    "留出返工的时间，手作急不得",
                    "做完记录下用料和时间，第二次会顺很多"}
    );

    private static final String[] TITLE_PATTERNS = {
            "%s｜我的真实体验",
            "%s的%d个实用思路",
            "关于%s，我想说几句真心话",
            "%s避坑指南：新手一定要看",
            "低成本%s计划，预算不到%d百",
            "%s清单：有这几样就够了",
            "%s入门：从零开始的完整记录",
            "坚持%s一个月后，我的变化",
            "%s复盘：做对了什么，也踩过哪些坑",
            "写给刚开始%s的你"
    };

    private static final String[] COMMENTS = {
            "写得很实在，收藏了", "这个思路我还没试过，周末试试", "第三点特别认同",
            "配图好好看，求原图", "刚好在找这类的经验，谢谢分享", "看完立刻想去实践一下",
            "细节写得真清楚，比很多教程都实用", "同款爱好者，握手", "学到了，感谢",
            "这条建议对我很有用", "已经有画面感了", "收藏夹又多了一条"
    };

    /** 详情页演示用的长评论句库（填充 200~300 条评论区） */
    private static final String[] RICH_COMMENTS = {
            "写得很实在，尤其是把细节和踩过的坑都写出来了，收藏了慢慢看",
            "这个思路我之前完全没想到，周末打算照着试一遍，有结果回来汇报",
            "第三点特别认同，我也是这么做的，效果确实不错",
            "配图好好看，配色很舒服，请问是用什么拍的",
            "刚好在找这类的经验，谢谢分享，解决了我一直纠结的问题",
            "看完立刻想去实践一下，感觉门槛没有想象中那么高",
            "细节写得真清楚，比很多教程都实用，希望能出续集",
            "同款爱好者，握手，我也在慢慢摸索这一块",
            "学到了，感谢，已经转给朋友一起看了",
            "这条建议对我很有用，之前一直走弯路，现在方向清楚多了",
            "已经有画面感了，仿佛跟着你走了一遍整个流程",
            "收藏夹又多了一条，希望以后能多分享这种干货",
            "看完有种被治愈的感觉，生活里这些小确幸真好",
            "排版和文字都很舒服，读起来一点也不累",
            "请问预算大概是多少，想照着做一个低配版本",
            "这个角度很新颖，之前看别人写都没提到这一点",
            "感谢分享，正好最近在整理类似的东西，很有参考价值",
            "写得好细腻，能感觉到你是真的喜欢这件事",
            "已经按你说的试了两天，确实比之前顺手多了",
            "求推荐入门清单，想从最简单的一步开始",
            "看到这条评论的人都去实践一下吧，真的不难",
            "文笔真好，读完心里暖暖的，谢谢你的分享",
            "我也有类似的经历，看到你写出来特别有共鸣",
            "这个小技巧太实用了，今天就用上了",
            "整体思路很清晰，一步一步跟着做完全没问题",
            "希望多更新这种日常向的内容，比纯教程更亲切",
            "第一次看到有人把这个讲得这么明白，厉害",
            "已经收藏加关注了，期待你的下一篇",
            "刚好周末有空，准备照着清单去逛一圈",
            "这个配色我可以抄作业吗，太好看了",
            "分享得很真诚，没有任何广告感，舒服",
            "照着做了之后朋友都说变化很大，回来谢谢你",
            "很治愈的一篇，焦虑的时候读一读心情会变好",
            "细节控狂喜，连小配件都写得清清楚楚",
            "求问这种搭配适合小个子吗，想试试",
            "从标题点进来，读完没失望，内容比标题还好",
            "把复杂的事情讲简单了，这才是真正的功力",
            "感谢你把这些经验留下来，对后来人太友好了",
            "看完了，默默点了个收藏，准备慢慢消化",
            "这条写进我的年度清单了，争取今年完成"
    };

    private static final String[] CHAT = {
            "在吗？看了你那篇灵感，写得好棒", "哈哈谢谢，最近在折腾这个", "你是用什么拍的？",
            "手机直出，调了一下色温", "我周末也想去试试", "那地方早上人少，建议早点去",
            "好，我记下了", "要不要一起去？", "可以啊，正好我也想再拍一组", "那就周六上午吧",
            "到时候我发你定位", "对了，上次你说的那本书我在看了", "感觉怎么样？", "前两章有点慢，后面越看越顺",
            "那我继续读下去", "回头整理个笔记一起交流", "好呀，期待", "最近更新还挺勤的",
            "灵感来得快就多写点", "一起加油"
    };

    @Override
    public void run(ApplicationArguments args) {
        try {
            seed();
        } catch (Exception e) {
            log.error("[DemoSeeder] 生成演示数据失败", e);
        }
    }

    private void seed() {
        String adminPwd = ensureAdminPassword();
        if (adminPwd == null) {
            log.warn("[DemoSeeder] 无法准备 admin 账号，跳过演示数据生成");
            return;
        }

        Map<String, Long> userIds = new LinkedHashMap<>();
        for (int i = 0; i < NEW_USERS.length; i++) {
            String[] u = NEW_USERS[i];
            long id = USER_ID + i;
            userIds.put(u[0], ensureUser(id, u[0], adminPwd, u[0] + "@demo.local", u[2], u[1], u[3]));
        }
        log.info("[DemoSeeder] 用户就绪: {}", userIds);

        List<Long> allUsers = new ArrayList<>(userIds.values());

        // 先把演示图生成进 MinIO，后面的灵感直接引用（不依赖外部图源）
        ensureDemoImages();
        loadCategoryChoices();

        Map<Long, List<Long>> userFolders = new HashMap<>();
        for (Long uid : allUsers) {
            userFolders.put(uid, ensureFolders(uid));
        }

        for (int i = 0; i < NEW_USERS.length; i++) {
            String[] u = NEW_USERS[i];
            Long ownerId = userIds.get(u[0]);
            int want = Integer.parseInt(u[4]);
            int exist = countInspires(ownerId);
            if (exist >= want) {
                log.info("[DemoSeeder] {} 已有 {} 条灵感，跳过生成", u[0], exist);
                continue;
            }
            generateForUser(ownerId, want, allUsers, userFolders);
        }

        // 先补详情页评论，保证后面任何一步异常都不会影响详情页演示数据
        ensureRichComments(allUsers);
        ensureFollows(allUsers);
        ensureConversations(allUsers);
        generateNotifications();

        // 种子完成后立即同步 ES，避免搜索功能等下一次定时任务才可用。
        esSyncService.batchSync();
        log.info("[DemoSeeder] 演示数据生成完成");
    }

    /**
     * 准备 admin 账号并返回其密码 hash（演示账号统一用这个密码）。
     *
     * <p>原来这里只「读取」admin 的密码：而 reset-data.sh 会清空整库，重置后库里根本没有 admin，
     * 种子就会整体跳过、什么都不生成（表现为 user 表 0 行）。
     * 现在改成：没有 admin 就自动建一个，保证一条 reset 命令就能得到可用系统。
     */
    private String ensureAdminPassword() {
        try {
            String pwd = jdbcTemplate.queryForObject(
                    "SELECT password FROM `user` WHERE LOWER(username) = 'admin' AND deleted = 0 LIMIT 1",
                    String.class);
            if (pwd != null && !pwd.isBlank()) {
                return pwd;
            }
        } catch (Exception ignored) {
            // 没查到，走下面的自动创建
        }
        try {
            String encoded = PASSWORD_ENCODER.encode(demoAdminPassword);
            jdbcTemplate.update(
                    "INSERT INTO `user`(id, username, password, email, avatar, nickname, city, deleted) "
                            + "VALUES(?,?,?,?,?,?,?,0)",
                    ADMIN_ID, "admin", encoded, "admin@demo.local", "🌙", "甜蜜小鹿", "杭州");
            log.warn("[DemoSeeder] 库里没有 admin 账号，已自动创建：admin / {}"
                    + "（可用环境变量 INSPIRE_DEMO_ADMIN_PASSWORD 覆盖）", demoAdminPassword);
            return encoded;
        } catch (Exception e) {
            log.warn("[DemoSeeder] 自动创建 admin 账号失败: {}", e.getMessage());
            return null;
        }
    }

    private long ensureUser(long id, String username, String password, String email,
                            String avatar, String nickname, String city) {
        List<Long> found = jdbcTemplate.queryForList(
                "SELECT id FROM `user` WHERE username = ?", Long.class, username);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        jdbcTemplate.update(
                "INSERT INTO `user`(id, username, password, email, avatar, nickname, city, deleted) "
                        + "VALUES(?,?,?,?,?,?,?,0)",
                id, username, password, email, avatar, nickname, city);
        log.info("[DemoSeeder] 新建用户 {} (id={})", username, id);
        return id;
    }

    private int countInspires(Long userId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE user_id = ? AND deleted = 0", Integer.class, userId);
        return c == null ? 0 : c;
    }

    private List<Long> ensureFolders(Long userId) {
        List<Long> exist = jdbcTemplate.queryForList(
                "SELECT id FROM collect_folder WHERE user_id = ? ORDER BY sort_order", Long.class, userId);
        if (!exist.isEmpty()) {
            return exist;
        }
        String[][] defs = {
                {"家居灵感", "🏠"}, {"美食收藏", "🍜"}, {"旅行清单", "🏕"},
                {"摄影参考", "📷"}, {"穿搭灵感", "👗"}, {"未分类", "📂"}
        };
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < defs.length; i++) {
            CollectFolder f = new CollectFolder();
            f.setId(FOLDER_ID + (folderSeq++));
            f.setUserId(userId);
            f.setName(defs[i][0]);
            f.setIcon(defs[i][1]);
            f.setSortOrder(i);
            f.setCreateTime(now());
            folderMapper.insert(f);
            ids.add(f.getId());
        }
        return ids;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    // ==================== 灵感 + 互动 ====================

    private void loadCategoryChoices() {
        categoryChoices.clear();
        Map<Long, String> parents = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT id, name FROM sys_category "
                        + "WHERE parent_id = 0 AND status = 1 AND deleted = 0 "
                        + "ORDER BY sort_order, id",
                rs -> { parents.put(rs.getLong("id"), rs.getString("name")); });

        for (Map.Entry<Long, String> parent : parents.entrySet()) {
            Long parentId = parent.getKey();
            String parentName = parent.getValue();
            jdbcTemplate.query(
                    "SELECT id, name FROM sys_category "
                            + "WHERE parent_id = ? AND status = 1 AND deleted = 0 "
                            + "ORDER BY sort_order, id",
                    rs -> {
                        categoryChoices.add(new CategoryChoice(
                                parentId, parentName, rs.getLong("id"), rs.getString("name")));
                    },
                    parentId);
        }
        if (categoryChoices.isEmpty()) {
            throw new IllegalStateException("sys_category 没有可用二级分类，拒绝生成虚空种子数据");
        }
        log.info("[DemoSeeder] 已加载后台分类组合 {} 组", categoryChoices.size());
    }

    private void generateForUser(Long ownerId, int want, List<Long> allUsers,
                                 Map<Long, List<Long>> userFolders) {
        List<Long> others = new ArrayList<>();
        for (Long u : allUsers) {
            if (!u.equals(ownerId)) others.add(u);
        }

        log.info("[DemoSeeder] 开始为用户 {} 生成 {} 条灵感…", ownerId, want);
        for (int i = 0; i < want; i++) {
            CategoryChoice category = categoryChoices.get(rnd.nextInt(categoryChoices.size()));
            String tag = category.categoryName();
            String topic = category.subCategoryName();

            long inspireId = INSPIRE_ID + (inspireSeq++);
            LocalDateTime createTime = now()
                    .minusDays(rnd.nextInt(365))
                    .minusMinutes(rnd.nextInt(1440));
            long view = 30 + rnd.nextInt(3000);

            // 先算互动再落库，计数一次写进主表，省掉 1000 次 UPDATE
            List<Long> likers = new ArrayList<>();
            List<Long> collectors = new ArrayList<>();
            for (Long actor : others) {
                if (rnd.nextInt(100) < 85) likers.add(actor);
                if (rnd.nextInt(100) < 85) collectors.add(actor);
            }
            int likeCnt = likers.size();
            int collectCnt = collectors.size();

            String img = demoImage(inspireId);
            InspireMain m = new InspireMain();
            m.setId(inspireId);
            m.setTitle(buildTitle(topic));
            m.setImg(img);
            m.setImages("[\"" + img + "\",\"" + demoImage(inspireId + 7) + "\"]");
            m.setTag(tag);
            m.setCategoryId(category.categoryId());
            m.setSubCategoryId(category.subCategoryId());
            m.setUserId(ownerId);
            m.setStatus(1);
            m.setViewCount(view);
            m.setLikeCount(likeCnt);
            m.setCollectCount(collectCnt);
            m.setHeat((int) (view + likeCnt * 10L + collectCnt * 20L));
            m.setShareCount(rnd.nextInt(12));
            m.setPublishCity(CITIES[rnd.nextInt(CITIES.length)]);
            m.setCreateTime(createTime);
            mainMapper.insert(m);

            InspireContent content = new InspireContent();
            content.setInspireId(inspireId);
            content.setContent(buildContent(tag, topic));
            contentMapper.insert(content);

            // 点赞明细：按 user_id % 10 分表
            for (Long actor : likers) {
                ShardContext.setByUserId(actor);
                try {
                    LikeAction a = new LikeAction();
                    a.setId(LIKE_ID + (likeSeq++));
                    a.setUserId(actor);
                    a.setInspireId(inspireId);
                    a.setCreateTime(createTime.plusMinutes(5 + rnd.nextInt(600)));
                    likeMapper.insert(a);
                } finally {
                    ShardContext.clear();
                }
            }

            // 收藏明细：按 user_id % 10 分表，随机落到该用户的某个收藏夹
            for (Long actor : collectors) {
                List<Long> folders = userFolders.get(actor);
                if (folders == null || folders.isEmpty()) continue;
                ShardContext.setByUserId(actor);
                try {
                    CollectAction a = new CollectAction();
                    a.setId(COLLECT_ID + (collectSeq++));
                    a.setUserId(actor);
                    a.setInspireId(inspireId);
                    a.setFolderId(folders.get(rnd.nextInt(folders.size())));
                    a.setCreateTime(createTime.plusMinutes(10 + rnd.nextInt(900)));
                    collectMapper.insert(a);
                } finally {
                    ShardContext.clear();
                }
            }

            // 评论 0~3 条
            int commentCnt = rnd.nextInt(4);
            for (int k = 0; k < commentCnt; k++) {
                Long actor = others.get(rnd.nextInt(others.size()));
                InspireComment cm = new InspireComment();
                cm.setId(COMMENT_ID + (commentSeq++));
                cm.setInspireId(inspireId);
                cm.setUserId(actor);
                cm.setUsername(nickOf(actor));
                cm.setAvatar("");
                cm.setParentId(0L);
                cm.setReplyUserId(0L);
                cm.setReplyUsername("");
                cm.setContent(COMMENTS[rnd.nextInt(COMMENTS.length)]);
                cm.setLikeCount(rnd.nextInt(12));
                cm.setCreateTime(createTime.plusMinutes(20 + rnd.nextInt(2000)));
                ShardContext.setByInspireId(inspireId);
                try {
                    commentMapper.insert(cm);
                } finally {
                    ShardContext.clear();
                }
            }

            if ((i + 1) % 100 == 0) {
                log.info("[DemoSeeder] 用户 {} 已生成 {}/{} 条", ownerId, i + 1, want);
            }
        }
    }

    // ==================== 详情页 200~300 条评论填充 ====================

    /**
     * 为详情页准备足够的评论数据（200~300 条），让分页、回复链都能演示。
     *
     * 幂等：某条灵感评论数已达到下限就跳过，重复启动不会重复灌。
     * 目标灵感 = 指定重点灵感（当前演示详情页）+ 每个演示用户热度最高的若干条灵感。
     */
    private void ensureRichComments(List<Long> allUsers) {
        Set<Long> targets = new LinkedHashSet<>();
        for (long id : FEATURED_COMMENT_INSPIRES) {
            targets.add(id);
        }
        for (Long uid : allUsers) {
            targets.addAll(jdbcTemplate.queryForList(
                    "SELECT id FROM inspire_main WHERE user_id = ? AND deleted = 0 "
                            + "ORDER BY heat DESC LIMIT " + RICH_COMMENT_PER_USER,
                    Long.class, uid));
        }

        Map<Long, String[]> profiles = loadUserProfiles();
        long nextId = nextCommentId();
        int processed = 0, inserted = 0;
        for (Long inspireId : targets) {
            if (!inspireExists(inspireId)) {
                continue;
            }
            int exist = countComments(inspireId);
            if (exist >= RICH_COMMENT_MIN) {
                continue;
            }
            int target = RICH_COMMENT_MIN + rnd.nextInt(RICH_COMMENT_MAX - RICH_COMMENT_MIN + 1);
            int added = fillComments(inspireId, target - exist, profiles, nextId);
            nextId += added;
            inserted += added;
            processed++;
        }
        log.info("[DemoSeeder] 详情页评论填充完成：处理 {} 条灵感，新增 {} 条评论", processed, inserted);
    }

    /** user_id -> [nickname, avatar]，评论落库时冗余昵称、头像，避免读取时再逐条查库 */
    private Map<Long, String[]> loadUserProfiles() {
        Map<Long, String[]> profiles = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT id, nickname, avatar FROM `user` WHERE deleted = 0", rs -> {
            profiles.put(rs.getLong("id"),
                    new String[]{ rs.getString("nickname"), rs.getString("avatar") });
        });
        return profiles;
    }

    private long nextCommentId() {
        StringBuilder union = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) union.append(" UNION ALL ");
            union.append("SELECT COALESCE(MAX(id), 0) AS id FROM inspire_comment_").append(i);
        }
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM (" + union + ") t", Long.class);
        return (max == null ? 0L : max) + 1L;
    }

    private int countComments(Long inspireId) {
        int shard = (int) Math.floorMod(inspireId, 10);
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_comment_" + shard
                        + " WHERE inspire_id = ? AND deleted = 0",
                Integer.class, inspireId);
        return c == null ? 0 : c;
    }

    private boolean inspireExists(Long inspireId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspire_main WHERE id = ? AND deleted = 0",
                Integer.class, inspireId);
        return c != null && c > 0;
    }

    /**
     * 生成 need 条评论：约 55% 为主评论，其余为挂在主评论下的回复，
     * 回复会带上 replyUserId / replyUsername，前端即可展示「A 回复 B」。
     */
    private int fillComments(Long inspireId, int need, Map<Long, String[]> profiles, long startId) {
        if (need <= 0 || profiles.isEmpty()) {
            return 0;
        }
        List<Long> actors = new ArrayList<>(profiles.keySet());
        List<Object[]> batch = new ArrayList<>(need);
        List<Long> commentIds = new ArrayList<>();
        List<Long> commentUsers = new ArrayList<>();
        List<Long> commentRootIds = new ArrayList<>();

        LocalDateTime cursor = now().minusDays(25).withHour(8).withMinute(0).withSecond(0).withNano(0);
        long id = startId;
        for (int i = 0; i < need; i++) {
            cursor = cursor.plusMinutes(1 + rnd.nextInt(180));
            Long actor = actors.get(rnd.nextInt(actors.size()));
            String[] actorProfile = profiles.get(actor);

            long parentId = 0L, replyUserId = 0L;
            String replyUsername = "";
            boolean asReply = !commentIds.isEmpty() && rnd.nextInt(100) < 48;
            if (asReply) {
                int idx = rnd.nextInt(commentIds.size());
                parentId = commentRootIds.get(idx);
                Long targetUser = commentUsers.get(idx);
                replyUserId = targetUser;
                replyUsername = nicknameOf(profiles.get(targetUser), targetUser);
            }

            Timestamp ts = Timestamp.valueOf(cursor);
            int likeCount = randomCommentLikes(asReply);
            batch.add(new Object[]{
                    id, inspireId, actor, nicknameOf(actorProfile, actor),
                    actorProfile == null || actorProfile[1] == null ? "" : actorProfile[1],
                    parentId, replyUserId, replyUsername,
                    RICH_COMMENTS[rnd.nextInt(RICH_COMMENTS.length)], likeCount, ts, ts
            });
            commentIds.add(id);
            commentUsers.add(actor);
            commentRootIds.add(asReply ? parentId : id);
            id++;
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO inspire_comment_" + Math.floorMod(inspireId, 10)
                        + "(id, inspire_id, user_id, username, avatar, parent_id, "
                        + "reply_user_id, reply_username, content, like_count, create_time, update_time, deleted) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)",
                batch);
        return need;
    }

    private int randomCommentLikes(boolean reply) {
        int roll = rnd.nextInt(100);
        if (reply) {
            return rnd.nextInt(10);
        }
        if (roll < 10) return 50 + rnd.nextInt(120);
        if (roll < 42) return 12 + rnd.nextInt(38);
        return 10 + rnd.nextInt(8);
    }

    private String nicknameOf(String[] profile, Long userId) {
        if (profile != null && profile[0] != null && !profile[0].isBlank()) {
            return profile[0];
        }
        return nickOf(userId);
    }

    private String buildTitle(String topic) {
        String p = TITLE_PATTERNS[rnd.nextInt(TITLE_PATTERNS.length)];
        String s = String.format(p, topic, 3 + rnd.nextInt(7));
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    /** 正文 3~5 段 + 3 条编号建议，约 250~400 字 */
    private String buildContent(String tag, String topic) {
        String[] pool = SENTENCES.getOrDefault(tag, SENTENCES.get("生活"));
        StringBuilder sb = new StringBuilder();
        sb.append("最近一直在琢磨「").append(topic).append("」这件事，边做边记，攒下了一些可以直接照做的经验。\n\n");
        sb.append(pick(pool)).append("。").append(pick(pool)).append("。\n\n");
        sb.append("具体可以分三步走：\n");
        sb.append("1. ").append(pick(pool)).append("。\n");
        sb.append("2. ").append(pick(pool)).append("。\n");
        sb.append("3. ").append(pick(pool)).append("。\n\n");
        sb.append(pick(pool)).append("。");
        return sb.toString();
    }

    private String pick(String[] pool) {
        return pool[rnd.nextInt(pool.length)];
    }

    private String picsum(long seed) {
        return "https://picsum.photos/seed/inspire" + seed + "/800/600";
    }

    // ==================== 演示图片：本地生成 + 存入 MinIO ====================

    /** 预生成多少张演示图；灵感按 ID 轮询复用，避免往 MinIO 灌两千张图 */
    private static final int DEMO_IMAGE_COUNT = 60;
    private static final String DEMO_IMAGE_PREFIX = "upload/demo/";

    /** 已就绪的演示图访问地址（公开图片直接走 CDN） */
    private final List<String> demoImageUrls = new ArrayList<>();

    /**
     * 确保 MinIO 里有演示图。图片由代码本地画出来，不依赖 picsum 等外部图源，
     * 这样演示环境完全自包含，离线也能看到图。
     */
    private void ensureDemoImages() {
        if (!demoImageUrls.isEmpty()) return;
        for (int i = 0; i < DEMO_IMAGE_COUNT; i++) {
            String key = DEMO_IMAGE_PREFIX + i + ".jpg";
            try {
                if (!demoImageExists(key)) {
                    byte[] jpg = buildPlaceholderJpeg(i);
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(key)
                            .stream(new ByteArrayInputStream(jpg), jpg.length, -1)
                            .contentType("image/jpeg")
                            .build());
                }
                Map<Integer, String> variants = imageVariantService.ensureMinioVariants(key);
                demoImageUrls.add(variants.getOrDefault(
                        800, cdnDomain.replaceAll("/+$", "") + "/" + key + "?v=2"));
            } catch (Exception e) {
                log.warn("[DemoSeeder] 演示图生成失败 key={}: {}", key, e.getMessage());
            }
        }
        log.info("[DemoSeeder] 演示图就绪 {} 张（已存入 MinIO）", demoImageUrls.size());
    }

    private boolean demoImageExists(String key) {
        try {
            minioClient.statObject(io.minio.StatObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 生成一张确定性的渐变占位图 */
    private byte[] buildPlaceholderJpeg(int index) throws Exception {
        int w = 800, h = 600;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float hue = (index * 37f) % 360f / 360f;
        g.setPaint(new java.awt.GradientPaint(0, 0, Color.getHSBColor(hue, 0.32f, 0.96f),
                w, h, Color.getHSBColor((hue + 0.12f) % 1f, 0.45f, 0.72f)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 55));
        for (int x = -h; x < w; x += 90) {
            g.fillPolygon(new int[]{x, x + 30, x + 30 + h, x + h}, new int[]{0, 0, h, h}, 4);
        }
        g.setColor(new Color(255, 255, 255, 165));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 42));
        g.drawString("Inspire " + (index + 1), 48, h - 54);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    /** 按灵感 ID 取一张演示图（轮询复用） */
    private String demoImage(long seed) {
        if (demoImageUrls.isEmpty()) return "";
        int idx = (int) Math.abs(seed % demoImageUrls.size());
        return demoImageUrls.get(idx);
    }

    private String nickOf(Long uid) {
        for (int i = 0; i < NEW_USERS.length; i++) {
            if (USER_ID + i == uid) return NEW_USERS[i][1];
        }
        return "灵感爱好者";
    }

    // ==================== 关注 / 私信 / 通知 ====================

    private void ensureFollows(List<Long> users) {
        for (Long a : users) {
            for (Long b : users) {
                if (a.equals(b)) continue;
                Integer c = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_follow WHERE follower_id = ? AND followee_id = ?",
                        Integer.class, a, b);
                if (c != null && c > 0) continue;
                jdbcTemplate.update(
                        "INSERT INTO user_follow(id, follower_id, followee_id, create_time) VALUES(?,?,?,?)",
                        FOLLOW_ID + (followSeq++), a, b, now().minusDays(rnd.nextInt(60)));
            }
        }
    }

    private void ensureConversations(List<Long> users) {
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                Long u1 = users.get(i);
                Long u2 = users.get(j);
                Integer c = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM message_conversation "
                                + "WHERE (user1_id=? AND user2_id=?) OR (user1_id=? AND user2_id=?)",
                        Integer.class, u1, u2, u2, u1);
                if (c != null && c > 0) continue;

                long convId = CONV_ID + (convSeq++);
                int count = 10 + rnd.nextInt(11);            // 10~20 条
                LocalDateTime t = now().minusDays(15 - i * 3L).withHour(9).withMinute(0).withSecond(0).withNano(0);
                String last = "";
                Long lastFrom = u1;

                for (int k = 0; k < count; k++) {
                    Long from = (k % 2 == 0) ? u1 : u2;
                    Long to = from.equals(u1) ? u2 : u1;
                    String text = CHAT[rnd.nextInt(CHAT.length)];

                    Message msg = new Message();
                    msg.setId(MSG_ID + (msgSeq++));
                    msg.setConversationId(convId);
                    msg.setFromUserId(from);
                    msg.setToUserId(to);
                    msg.setContent(text);
                    msg.setCreateTime(t);
                    messageMapper.insert(msg);

                    last = text;
                    lastFrom = from;
                    t = t.plusMinutes(3 + rnd.nextInt(120));
                }

                MessageConversation conv = new MessageConversation();
                conv.setId(convId);
                conv.setUser1Id(u1);
                conv.setUser2Id(u2);
                conv.setLastContent(last);
                conv.setLastTime(t.minusMinutes(5));
                conv.setUnreadUser1(lastFrom.equals(u1) ? 0 : 1);
                conv.setUnreadUser2(lastFrom.equals(u2) ? 0 : 1);
                conv.setCreateTime(t.minusDays(1));
                conv.setUpdateTime(t);
                conversationMapper.insert(conv);
                jdbcTemplate.update(
                        "INSERT INTO conversation_member(id, conversation_id, user_id, unread_count, last_time, create_time, update_time) "
                                + "VALUES(?,?,?,?,?,?,?)",
                        MEMBER_ID + (memberSeq++), convId, u1, conv.getUnreadUser1(),
                        conv.getLastTime(), conv.getCreateTime(), conv.getUpdateTime());
                jdbcTemplate.update(
                        "INSERT INTO conversation_member(id, conversation_id, user_id, unread_count, last_time, create_time, update_time) "
                                + "VALUES(?,?,?,?,?,?,?)",
                        MEMBER_ID + (memberSeq++), convId, u2, conv.getUnreadUser2(),
                        conv.getLastTime(), conv.getCreateTime(), conv.getUpdateTime());
            }
        }
    }

    /** 通知：取最近的灵感，每用户最多 50 条，避免通知表被灌成几万行 */
    private void generateNotifications() {
        Integer existed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_notification WHERE id >= ?", Integer.class, NOTIFY_ID);
        if (existed != null && existed > 0) {
            log.info("[DemoSeeder] 通知已存在 {} 条，跳过生成", existed);
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id AS owner, id AS inspire_id, title, create_time "
                        + "FROM inspire_main WHERE deleted = 0 AND user_id >= ? "
                        + "ORDER BY create_time DESC LIMIT 50",
                USER_ID);

        List<Long> users = new ArrayList<>();
        for (int i = 0; i < NEW_USERS.length; i++) users.add(USER_ID + i);

        for (Map<String, Object> r : rows) {
            Long owner = ((Number) r.get("owner")).longValue();
            Long inspireId = ((Number) r.get("inspire_id")).longValue();
            String title = String.valueOf(r.get("title"));
            String shortTitle = title.length() > 20 ? title.substring(0, 20) + "..." : title;
            // MySQL Connector/J 对 DATETIME 会直接返回 LocalDateTime，老驱动才返回 Timestamp
            Object rawTime = r.get("create_time");
            LocalDateTime t = rawTime instanceof LocalDateTime
                    ? (LocalDateTime) rawTime
                    : ((Timestamp) rawTime).toLocalDateTime();

            List<Long> actors = new ArrayList<>();
            for (Long u : users) {
                if (!u.equals(owner)) actors.add(u);
            }
            if (actors.isEmpty()) continue;

            Long actor = actors.get(rnd.nextInt(actors.size()));
            String[] types = {"like", "collect", "comment"};
            String type = types[rnd.nextInt(types.length)];
            String content = "like".equals(type) ? "点赞了你的灵感"
                    : "collect".equals(type) ? "收藏了你的灵感" : "评论了你的灵感";

            jdbcTemplate.update(
                    "INSERT INTO user_notification(id,user_id,type,actor_id,actor_name,content,"
                            + "target_id,target_title,is_read,deleted,create_time) VALUES(?,?,?,?,?,?,?,?,?,0,?)",
                    NOTIFY_ID + (notifySeq++), owner, type, actor, nickOf(actor),
                    content, inspireId, shortTitle, rnd.nextInt(3) == 0 ? 0 : 1, t.plusMinutes(30));
        }
        log.info("[DemoSeeder] 通知生成 {} 条", notifySeq);
    }
}
