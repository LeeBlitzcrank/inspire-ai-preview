# AI灵感分享平台 全套MySQL建表SQL脚本 + 字段说明文档
## 前置说明
1. 数据库引擎：InnoDB，字符集 utf8mb4，排序 utf8mb4_unicode_ci，支持Emoji
2. 主键统一采用雪花算法 BIGINT，不自增，适配分库分表
3. 全部业务表开启逻辑删除 `deleted TINYINT DEFAULT 0`，不物理删除数据
4. 冷热分离：`inspire_main`（高频列表查询）、`inspire_content`（长文本冷数据，仅详情查询）
5. 收藏、点赞采用分表模板，按ID取模拆分10张子表（0~9），缓解单表千万数据压力
6. 扩展字段统一 `ext_json JSON`，新增业务字段无需DDL改表
7. 配套表：用户表、注册日志、行为日志、用户离线画像、运营配置表

## 一、全局统一注释规范
- `id`：雪花全局唯一主键 BIGINT NOT NULL
- `create_time`：创建时间 DATETIME DEFAULT CURRENT_TIMESTAMP
- `update_time`：更新时间 DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- `deleted`：逻辑删除 0正常 1删除 TINYINT DEFAULT 0
- `ext_json`：扩展JSON字段，预留业务拓展 JSON NULL

---

# 完整建表SQL（可直接批量执行）
```sql
-- 1. 用户主表 user
CREATE TABLE `user` (
  `id` BIGINT NOT NULL COMMENT '雪花用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，唯一不可重复',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '用户头像URL',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '用户昵称',
  `city` VARCHAR(32) DEFAULT '' COMMENT '常居城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除',
  `ext_json` JSON NULL COMMENT '扩展字段：性别、生日、个性签名等',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

-- 2. 用户注册日志表 user_register_log
CREATE TABLE `user_register_log` (
  `id` BIGINT NOT NULL COMMENT '雪花日志ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `register_ip` VARCHAR(64) DEFAULT '' COMMENT '注册IP地址',
  `register_city` VARCHAR(32) DEFAULT '' COMMENT '注册城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户注册行为日志';

-- 3. 灵感主表 inspire_main 冷热分离-高频查询表（列表、首页、搜索）
CREATE TABLE `inspire_main` (
  `id` BIGINT NOT NULL COMMENT '雪花灵感ID',
  `title` VARCHAR(120) NOT NULL COMMENT '灵感标题',
  `img` VARCHAR(255) NOT NULL COMMENT '封面图片地址',
  `tag` VARCHAR(30) NOT NULL COMMENT '灵感分类：美食/运动/电影/穿搭/文案',
  `user_id` BIGINT NOT NULL COMMENT '发布人用户ID',
  `view_count` BIGINT DEFAULT 0 COMMENT '浏览量（Redis定时同步落库）',
  `like_count` INT DEFAULT 0 COMMENT '点赞总数',
  `collect_count` INT DEFAULT 0 COMMENT '收藏总数',
  `heat` INT DEFAULT 0 COMMENT '综合热度值（Flink实时计算）',
  `publish_city` VARCHAR(32) DEFAULT '' COMMENT '发布时定位城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1下架/删除',
  `ext_json` JSON NULL COMMENT '扩展字段：视频链接、难度标签等',
  PRIMARY KEY (`id`),
  KEY `idx_tag_deleted` (`tag`,`deleted`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感主表-冷热分离高频表';

-- 4. 灵感内容附表 inspire_content 冷热分离-冷数据表（仅详情打开查询）
CREATE TABLE `inspire_content` (
  `inspire_id` BIGINT NOT NULL COMMENT '关联灵感主表ID，一对一',
  `content` TEXT NOT NULL COMMENT '灵感完整长文本详情',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感详情文本附表（冷热分离）';

-- 5. 用户全量行为日志表 user_behavior_log（永久存储，Flink离线落地）
CREATE TABLE `user_behavior_log` (
  `id` BIGINT NOT NULL COMMENT '雪花日志主键',
  `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
  `behavior_type` TINYINT NOT NULL COMMENT '行为类型 1AI搜索 2选中灵感 3浏览详情 4点赞 5收藏 6发布灵感',
  `keyword` VARCHAR(120) DEFAULT '' COMMENT 'AI搜索关键词',
  `inspire_id` BIGINT DEFAULT NULL COMMENT '关联灵感ID，无则为NULL',
  `operate_city` VARCHAR(32) NOT NULL COMMENT '操作时定位城市',
  `stay_second` INT DEFAULT 0 COMMENT '详情停留秒数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_inspire_id` (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户全量行为日志';

-- 6. 用户离线画像表 user_profile（Flink每日批量落地Redis画像数据）
CREATE TABLE `user_profile` (
  `id` BIGINT NOT NULL COMMENT '雪花主键',
  `user_id` BIGINT NOT NULL COMMENT '用户唯一ID',
  `interest_json` JSON NOT NULL COMMENT '分类兴趣权重 {美食:48,运动:12}',
  `common_city` VARCHAR(32) DEFAULT '' COMMENT '高频活跃城市',
  `active_hour_json` JSON NULL COMMENT '高频活跃时段 [10,18,22]',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '画像更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣画像离线持久化表';

-- 7. 运营配置表 admin_config（管理员后台配置项）
CREATE TABLE `admin_config` (
  `id` INT AUTO_INCREMENT NOT NULL COMMENT '自增配置ID',
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置唯一key',
  `config_value` VARCHAR(500) DEFAULT '' COMMENT '配置值JSON/文本',
  `desc` VARCHAR(120) DEFAULT '' COMMENT '配置说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营配置表：推荐权重、黑名单、地图关键词';

-- 初始化地图分类映射配置
INSERT INTO admin_config(config_key,config_value,desc) VALUES
('map_tag_mapping','{"美食":"炸鸡店,火锅店","运动":"健身房,运动场馆","电影":"电影院","穿搭":"服装店","文案":"文创店"}','灵感分类对应地图检索关键词');

-- ====================== 分表模板：收藏表 collect_0 ~ collect_9 ======================
-- 分表路由规则：user_id % 10
CREATE TABLE `collect_0` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `inspire_id` BIGINT NOT NULL COMMENT '被收藏灵感ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`) COMMENT '联合唯一索引，防止重复收藏'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏分表0';

CREATE TABLE `collect_1` LIKE collect_0;
CREATE TABLE `collect_2` LIKE collect_0;
CREATE TABLE `collect_3` LIKE collect_0;
CREATE TABLE `collect_4` LIKE collect_0;
CREATE TABLE `collect_5` LIKE collect_0;
CREATE TABLE `collect_6` LIKE collect_0;
CREATE TABLE `collect_7` LIKE collect_0;
CREATE TABLE `collect_8` LIKE collect_0;
CREATE TABLE `collect_9` LIKE collect_0;

-- ====================== 分表模板：点赞表 inspire_like_0 ~ inspire_like_9 ======================
-- 分表路由规则：inspire_id % 10
CREATE TABLE `inspire_like_0` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `inspire_id` BIGINT NOT NULL COMMENT '被点赞灵感ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspire_user` (`inspire_id`,`user_id`) COMMENT '联合唯一索引，防止重复点赞'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感点赞分表0';

CREATE TABLE `inspire_like_1` LIKE inspire_like_0;
CREATE TABLE `inspire_like_2` LIKE inspire_like_0;
CREATE TABLE `inspire_like_3` LIKE inspire_like_0;
CREATE TABLE `inspire_like_4` LIKE inspire_like_0;
CREATE TABLE `inspire_like_5` LIKE inspire_like_0;
CREATE TABLE `inspire_like_6` LIKE inspire_like_0;
CREATE TABLE `inspire_like_7` LIKE inspire_like_0;
CREATE TABLE `inspire_like_8` LIKE inspire_like_0;
CREATE TABLE `inspire_like_9` LIKE inspire_like_0;
```

---

# 二、分表路由规则文档
## 1. 收藏分表 collect_{0~9}
路由计算：`user_id % 10`
- 用户ID 10001 → 10001 % 10 = 1 → 写入 collect_1
  查询我的收藏：根据当前登录用户ID取模，仅查询对应单表，不跨表扫描
  唯一约束：`uk_user_inspire` 数据库层杜绝同一用户重复收藏同一灵感

## 2. 点赞分表 inspire_like_{0~9}
路由计算：`inspire_id % 10`
- 灵感ID 20005 → 20005 % 10 = 5 → 写入 inspire_like_5
  查询灵感点赞列表：根据灵感ID取模，单表查询
  唯一约束：`uk_inspire_user` 杜绝重复点赞

## 3. 冷热分离查询规范
1. 首页、列表、分类分页、搜索：只查询 `inspire_main`，不关联 `inspire_content`，提升分页速度
2. 灵感详情页：LEFT JOIN `inspire_content` 获取完整正文
3. ES同步源：仅同步 `inspire_main`，正文单独关联附表展示

---

# 三、每张表完整字段说明文档
## 1. user 用户主表
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花用户唯一ID |
| username | VARCHAR(50) | 是 | 登录账号，唯一索引 |
| password | VARCHAR(100) | 是 | BCrypt加密密码，禁止明文存储 |
| avatar | VARCHAR(255) | 否 | 头像图片链接 |
| nickname | VARCHAR(50) | 否 | 展示昵称 |
| city | VARCHAR(32) | 否 | 用户常居城市 |
| create_time | DATETIME | 否 | 注册时间，默认当前 |
| update_time | DATETIME | 否 | 更新自动刷新 |
| deleted | TINYINT | 否 | 0正常 1删除，逻辑删除 |
| ext_json | JSON | 否 | 扩展字段：性别、生日、签名等 |

## 2. user_register_log 注册日志
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花日志ID |
| user_id | BIGINT | 是 | 关联用户ID |
| register_ip | VARCHAR(64) | 否 | 注册客户端IP |
| register_city | VARCHAR(32) | 否 | 注册定位城市 |
| create_time | DATETIME | 否 | 注册发生时间 |

## 3. inspire_main 灵感主表（冷热分离高频）
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花灵感ID |
| title | VARCHAR(120) | 是 | 灵感标题，AI生成/用户编辑 |
| img | VARCHAR(255) | 是 | 封面图URL |
| tag | VARCHAR(30) | 是 | 分类：美食/运动/电影/穿搭/文案 |
| user_id | BIGINT | 是 | 发布人用户ID |
| view_count | BIGINT | 否 | 浏览计数，Redis定时同步DB |
| like_count | INT | 否 | 点赞总数 |
| collect_count | INT | 否 | 收藏总数 |
| heat | INT | 否 | Flink实时计算综合热度值 |
| publishing_city | VARCHAR(32) | 否 | 发布时定位城市 |
| create_time | DATETIME | 否 | 发布时间 |
| update_time | DATETIME | 否 | 修改更新时间 |
| deleted | TINYINT | 否 | 0展示 1下架删除 |
| ext_json | JSON | 否 | 拓展属性预留 |

## 4. inspire_content 灵感正文附表（冷热分离冷数据）
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| inspire_id | BIGINT | 是 | 与主表id一对一，主键关联 |
| content | TEXT | 是 | 大文本灵感详情正文 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |

## 5. user_behavior_log 用户行为日志
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花日志主键 |
| user_id | BIGINT | 是 | 操作人ID |
| behavior_type | TINYINT | 是 | 1AI搜索 2选中灵感 3浏览 4点赞 5收藏 6发布 |
| keyword | VARCHAR(120) | 否 | AI搜索输入关键词 |
| inspire_id | BIGINT | 否 | 操作关联灵感ID，无则NULL |
| operate_city | VARCHAR(32) | 是 | 操作定位城市 |
| stay_second | INT | 否 | 详情页面停留秒数 |
| create_time | DATETIME | 否 | 行为发生时间 |

## 6. user_profile 用户离线画像表
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花主键 |
| user_id | BIGINT | 是 | 用户唯一ID，唯一索引 |
| interest_json | JSON | 是 | 各分类兴趣权重JSON对象 |
| common_city | VARCHAR(32) | 否 | 用户高频活跃城市 |
| active_hour_json | JSON | 否 | 每日活跃时段数组 [10,20,22] |
| update_time | DATETIME | 否 | 画像批量更新时间 |

## 7. admin_config 运营配置表
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | INT | 是 | 自增主键 |
| config_key | VARCHAR(50) | 是 | 配置唯一标识key |
| config_value | VARCHAR(500) | 否 | 配置内容，支持JSON |
| desc | VARCHAR(120) | 否 | 配置功能说明 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 修改时间 |

## 8. 收藏分表 collect_x 通用字段
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花ID |
| user_id | BIGINT | 是 | 收藏用户ID（分表路由键） |
| inspire_id | BIGINT | 是 | 被收藏灵感ID |
| create_time | DATETIME | 否 | 收藏时间 |
唯一索引 uk_user_inspire：同一用户不能重复收藏同一条灵感

## 9. 点赞分表 inspire_like_x 通用字段
| 字段名 | 类型 | 非空 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 雪花ID |
| user_id | BIGINT | 是 | 点赞用户ID |
| inspire_id | BIGINT | 是 | 灵感ID（分表路由键） |
| create_time | DATETIME | 否 | 点赞时间 |
唯一索引 uk_inspire_user：同一用户不可重复点赞同一条灵感

---

# 四、配套设计说明（开发落地要点）
1. **雪花ID生成**：所有业务表主键统一后端雪花工具生成，数据库不自增，支持未来分库分表无缝扩容
2. **冷热分离收益**：列表分页不读取TEXT大字段，千万级灵感分页查询性能提升50%+
3. **分表设计收益**：收藏、点赞高频写入表拆分10张子表，单表数据量可控百万内，避免大表索引失效、锁竞争
4. **联合唯一索引双层防重**：Redis前置拦截 + 数据库唯一索引兜底，高并发场景不会产生重复点赞/收藏
5. **ext_json扩展设计**：产品迭代新增字段无需执行ALTER TABLE，线上无锁表风险，适配快速迭代
6. **行为日志永久存储**：不做分表，全量行为数据用于Flink实时画像、离线数据分析、用户行为溯源
7. **计数异步落库**：浏览、点赞、收藏计数优先Redis缓存，定时任务批量更新MySQL，规避热点行频繁UPDATE锁表
-- 13. 用户通知表 user_notification
CREATE TABLE `user_notification` (
  `id` BIGINT NOT NULL COMMENT '雪花通知ID',
  `user_id` BIGINT NOT NULL COMMENT '通知接收者用户ID',
  `type` VARCHAR(16) NOT NULL COMMENT '通知类型: like/collect/comment/reply/follow',
  `actor_id` BIGINT NOT NULL COMMENT '触发者用户ID',
  `actor_name` VARCHAR(50) DEFAULT '' COMMENT '触发者昵称',
  `content` VARCHAR(255) DEFAULT '' COMMENT '通知摘要文本',
  `target_id` BIGINT DEFAULT NULL COMMENT '关联灵感/评论ID',
  `target_title` VARCHAR(120) DEFAULT '' COMMENT '关联灵感标题',
  `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表-消息系统';

-- 14. 灵感表新增分享计数字段
ALTER TABLE `inspire_main` ADD COLUMN `share_count` INT DEFAULT 0 COMMENT '分享次数' AFTER `collect_count`;
