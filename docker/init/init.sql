-- Inspire AI Preview Docker Init Script
-- 仅包含 CREATE TABLE IF NOT EXISTS，跳过 ALTER TABLE

CREATE TABLE IF NOT EXISTS `inspire_main` (
  `id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL COMMENT '灵感标题',
  `img` VARCHAR(255) DEFAULT '' COMMENT '封面图',
  `images` TEXT DEFAULT NULL COMMENT '多图JSON数组',
  `tag` VARCHAR(30) NOT NULL COMMENT '分类',
  `category_id` BIGINT DEFAULT NULL COMMENT '一级分类ID',
  `sub_category_id` BIGINT DEFAULT NULL COMMENT '二级分类ID',
  `user_id` BIGINT NOT NULL COMMENT '发布人ID',
  `status` TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
  `view_count` BIGINT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `collect_count` INT DEFAULT 0,
  `heat` INT DEFAULT 0,
  `share_count` INT DEFAULT 0 COMMENT '分享数',
  `publish_city` VARCHAR(32) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  `ext_json` JSON DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tag_deleted` (`tag`,`deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_time` (`status`,`create_time`),
  KEY `idx_category_status_time` (`category_id`,`sub_category_id`,`status`,`deleted`,`create_time`),
  KEY `idx_user_status_time` (`user_id`,`status`,`deleted`,`create_time`),
  KEY `idx_status_deleted_time` (`status`,`deleted`,`create_time`),
  KEY `idx_status_deleted_heat` (`status`,`deleted`,`heat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感主表';
CREATE TABLE IF NOT EXISTS `inspire_content` (
  `inspire_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL COMMENT '灵感正文',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感正文附表';
CREATE TABLE IF NOT EXISTS `collect_0`  ( `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `inspire_id` BIGINT NOT NULL, `folder_id` BIGINT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`), KEY `idx_user_folder_time` (`user_id`,`folder_id`,`create_time`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `collect_1`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_2`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_3`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_4`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_5`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_6`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_7`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_8`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_9`  LIKE `collect_0`;

-- ========== 新点赞分表 user_like_0 ~ user_like_9（按 user_id % 10） ==========
CREATE TABLE IF NOT EXISTS `user_like_0`  ( `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `inspire_id` BIGINT NOT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`), KEY `idx_inspire_user` (`inspire_id`,`user_id`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `user_like_1`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_2`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_3`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_4`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_5`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_6`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_7`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_8`  LIKE `user_like_0`;
CREATE TABLE IF NOT EXISTS `user_like_9`  LIKE `user_like_0`;

-- ========== 新评论分表 inspire_comment_0 ~ inspire_comment_9（按 inspire_id % 10） ==========
CREATE TABLE IF NOT EXISTS `inspire_comment_0` (
  `id` BIGINT NOT NULL,
  `inspire_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `username` VARCHAR(60) NOT NULL,
  `avatar` VARCHAR(255) DEFAULT '',
  `parent_id` BIGINT DEFAULT 0,
  `reply_user_id` BIGINT DEFAULT 0,
  `reply_username` VARCHAR(60) DEFAULT '',
  `content` VARCHAR(500) NOT NULL,
  `like_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_inspire_deleted_time` (`inspire_id`,`deleted`,`create_time`),
  KEY `idx_inspire_deleted_hot` (`inspire_id`,`deleted`,`like_count`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `inspire_comment_1` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_2` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_3` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_4` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_5` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_6` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_7` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_8` LIKE `inspire_comment_0`;
CREATE TABLE IF NOT EXISTS `inspire_comment_9` LIKE `inspire_comment_0`;

-- 评论点赞明细按 user_id % 10 分表，避免用户维度高频查询跨分片
CREATE TABLE IF NOT EXISTS `comment_like_0` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `comment_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `comment_like_1` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_2` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_3` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_4` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_5` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_6` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_7` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_8` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `comment_like_9` LIKE `comment_like_0`;
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT NOT NULL,
  `call_date` DATE NOT NULL,
  `keyword` VARCHAR(100) DEFAULT '',
  `user_id` BIGINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_call_date` (`call_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';
CREATE TABLE IF NOT EXISTS `user_follow` (
  `id` BIGINT NOT NULL,
  `follower_id` BIGINT NOT NULL COMMENT '关注者',
  `followee_id` BIGINT NOT NULL COMMENT '被关注者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`,`followee_id`),
  KEY `idx_follower` (`follower_id`),
  KEY `idx_followee` (`followee_id`),
  KEY `idx_follower_time` (`follower_id`,`create_time`),
  KEY `idx_followee_time` (`followee_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';
CREATE TABLE IF NOT EXISTS `collect_folder` (
  `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `name` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(10) DEFAULT '📁', `sort_order` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `message_conversation` (
  `id` BIGINT NOT NULL, `user1_id` BIGINT NOT NULL, `user2_id` BIGINT NOT NULL,
  `last_content` VARCHAR(500) DEFAULT '', `last_time` DATETIME,
  `unread_user1` INT DEFAULT 0, `unread_user2` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users` (`user1_id`, `user2_id`),
  KEY `idx_user1` (`user1_id`), KEY `idx_user2` (`user2_id`),
  KEY `idx_user1_time` (`user1_id`, `last_time`),
  KEY `idx_user2_time` (`user2_id`, `last_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL, `conversation_id` BIGINT NOT NULL,
  `from_user_id` BIGINT NOT NULL, `to_user_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conversation` (`conversation_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `conversation_member` (
  `id` BIGINT NOT NULL,
  `conversation_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `unread_count` INT DEFAULT 0,
  `last_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`,`user_id`),
  KEY `idx_user_time` (`user_id`,`last_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL COMMENT '雪花用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，唯一不可重复',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `email` VARCHAR(100) DEFAULT '' COMMENT '用户邮箱（必填，用于找回密码）',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '用户头像URL',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '用户昵称',
  `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色: admin/core/user',
  `city` VARCHAR(32) DEFAULT '' COMMENT '常居城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常 0冻结',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展字段：性别、生日、个性签名等',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';
CREATE TABLE IF NOT EXISTS `password_reset` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `email` VARCHAR(100) NOT NULL COMMENT '接收重置邮件的邮箱',
  `token` VARCHAR(64) NOT NULL COMMENT '重置令牌（UUID）',
  `expiry_time` DATETIME NOT NULL COMMENT '令牌过期时间',
  `used` TINYINT DEFAULT 0 COMMENT '是否已使用 0未使用 1已使用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';
CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` INT AUTO_INCREMENT NOT NULL,
  `username` VARCHAR(50) NOT NULL COMMENT '管理员账号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '管理员昵称',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';
CREATE TABLE IF NOT EXISTS `admin_config` (
  `id` INT AUTO_INCREMENT NOT NULL,
  PRIMARY KEY (`id`),
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置key',
  `config_value` VARCHAR(500) DEFAULT '' COMMENT '配置值',
  `desc` VARCHAR(120) DEFAULT '' COMMENT '配置说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营配置表';

-- ========== 以下表由应用代码创建，Docker 初始化时补充 ==========

CREATE TABLE IF NOT EXISTS `user_notification` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `actor_id` BIGINT NOT NULL,
  `actor_name` VARCHAR(60) NOT NULL,
  `content` VARCHAR(200) NOT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `target_title` VARCHAR(120) DEFAULT '',
  `is_read` TINYINT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_user_deleted_time` (`user_id`,`deleted`,`create_time`),
  KEY `idx_user_unread` (`user_id`,`deleted`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

CREATE TABLE IF NOT EXISTS `inspire_version` (
  `id` BIGINT NOT NULL,
  `inspire_id` BIGINT NOT NULL,
  `version_number` INT NOT NULL DEFAULT 0,
  `title` VARCHAR(120) DEFAULT '',
  `content` TEXT,
  `img` VARCHAR(255) DEFAULT '' COMMENT '封面图',
  `images` TEXT,
  `tag` VARCHAR(30) DEFAULT '',
  `change_summary` VARCHAR(500) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inspire_id` (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感版本历史';

-- =============================================
-- 图片上传元数据表（MinIO 文档第6章）
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_upload_image` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `file_key` VARCHAR(255) NOT NULL COMMENT 'MinIO文件路径',
  `original_name` VARCHAR(255) DEFAULT '' COMMENT '原始文件名',
  `cdn_url` VARCHAR(512) NOT NULL COMMENT 'CDN访问地址',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(20) DEFAULT '' COMMENT '文件类型（jpg/png/webp）',
  `mime_type` VARCHAR(64) DEFAULT '' COMMENT 'MIME类型',
  `width` INT DEFAULT 0 COMMENT '图片宽度（px）',
  `height` INT DEFAULT 0 COMMENT '图片高度（px）',
  `is_private` TINYINT DEFAULT 0 COMMENT '是否私有（0公开/1私有）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_key` (`file_key`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片上传元数据表';

-- ==================== 分类（两级）与 AI 探索词云 ====================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_category` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID，0 表示一级分类',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(20) DEFAULT '' COMMENT '图标（emoji）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感分类（两级）';

CREATE TABLE IF NOT EXISTS `sys_word_cloud` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `word` VARCHAR(50) NOT NULL COMMENT '词云词条',
  `weight` INT NOT NULL DEFAULT 1 COMMENT '权重，用于控制字号',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_word` (`word`),
  KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI探索词云词条';

-- 初始化分类（与前台原来的硬编码保持一致）
INSERT INTO `sys_category` (id, parent_id, name, icon, sort_order, status) VALUES
  (1001, 0, '美食', '🍜', 1, 1),
  (1002, 0, '运动', '🏃', 2, 1),
  (1003, 0, '电影', '🎬', 3, 1),
  (1004, 0, '穿搭', '👗', 4, 1),
  (1005, 0, '文案', '✍️', 5, 1),
  (1006, 0, '家居', '🏠', 6, 1),
  (1007, 0, '旅行', '🏕', 7, 1),
  (1008, 0, '摄影', '📷', 8, 1),
  (1009, 0, '生活', '🌿', 9, 1),
  (1010, 0, '手作', '🧶', 10, 1),
  (1101, 1001, '一人食快手菜', '', 1, 1),
  (1102, 1001, '周末煲汤', '', 2, 1),
  (1103, 1001, '空气炸锅', '', 3, 1),
  (1104, 1001, '早餐搭配', '', 4, 1),
  (1105, 1001, '手冲咖啡', '', 5, 1),
  (1106, 1001, '深夜食堂', '', 6, 1),
  (1107, 1001, '减脂餐', '', 7, 1),
  (1108, 1001, '烤箱甜点', '', 8, 1),
  (1201, 1002, '晨跑习惯', '', 1, 1),
  (1202, 1002, '居家训练', '', 2, 1),
  (1203, 1002, '爬山路线', '', 3, 1),
  (1204, 1002, '骑行通勤', '', 4, 1),
  (1205, 1002, '拉伸放松', '', 5, 1),
  (1206, 1002, '配速提升', '', 6, 1),
  (1207, 1002, '核心训练', '', 7, 1),
  (1208, 1002, '羽毛球', '', 8, 1),
  (1301, 1003, '老电影重看', '', 1, 1),
  (1302, 1003, '周末片单', '', 2, 1),
  (1303, 1003, '纪录片推荐', '', 3, 1),
  (1304, 1003, '冷门佳作', '', 4, 1),
  (1305, 1003, '导演风格', '', 5, 1),
  (1306, 1003, '配乐记录', '', 6, 1),
  (1307, 1003, '影院体验', '', 7, 1),
  (1308, 1003, '剧集短评', '', 8, 1),
  (1401, 1004, '秋冬叠穿', '', 1, 1),
  (1402, 1004, '极简衣橱', '', 2, 1),
  (1403, 1004, '通勤穿搭', '', 3, 1),
  (1404, 1004, '小个子比例', '', 4, 1),
  (1405, 1004, '配饰点缀', '', 5, 1),
  (1406, 1004, '色彩搭配', '', 6, 1),
  (1407, 1004, '基础款搭配', '', 7, 1),
  (1408, 1004, '换季整理', '', 8, 1),
  (1501, 1005, '朋友圈文案', '', 1, 1),
  (1502, 1005, '情绪记录', '', 2, 1),
  (1503, 1005, '读书笔记', '', 3, 1),
  (1504, 1005, '年度总结', '', 4, 1),
  (1505, 1005, '写信给自己', '', 5, 1),
  (1506, 1005, '短句收集', '', 6, 1),
  (1507, 1005, '日常碎碎念', '', 7, 1),
  (1508, 1005, '自我介绍', '', 8, 1),
  (1601, 1006, '小户型收纳', '', 1, 1),
  (1602, 1006, '灯光氛围', '', 2, 1),
  (1603, 1006, '原木风搭配', '', 3, 1),
  (1604, 1006, '租房改造', '', 4, 1),
  (1605, 1006, '阳台绿植', '', 5, 1),
  (1606, 1006, '厨房动线', '', 6, 1),
  (1607, 1006, '衣柜整理', '', 7, 1),
  (1608, 1006, '客厅配色', '', 8, 1),
  (1701, 1007, '秋日露营', '', 1, 1),
  (1702, 1007, '城市漫步', '', 2, 1),
  (1703, 1007, '周末短途', '', 3, 1),
  (1704, 1007, '一个人的旅行', '', 4, 1),
  (1705, 1007, '行李收纳', '', 5, 1),
  (1706, 1007, '青旅体验', '', 6, 1),
  (1707, 1007, '自驾路线', '', 7, 1),
  (1708, 1007, '小众目的地', '', 8, 1),
  (1801, 1008, '手机摄影', '', 1, 1),
  (1802, 1008, '胶片色调', '', 2, 1),
  (1803, 1008, '窗口人像', '', 3, 1),
  (1804, 1008, '街拍构图', '', 4, 1),
  (1805, 1008, '夜景拍摄', '', 5, 1),
  (1806, 1008, '静物布光', '', 6, 1),
  (1807, 1008, '旅行记录', '', 7, 1),
  (1808, 1008, '生活抓拍', '', 8, 1),
  (1901, 1009, '早起习惯', '', 1, 1),
  (1902, 1009, '记账复盘', '', 2, 1),
  (1903, 1009, '桌面整理', '', 3, 1),
  (1904, 1009, '情绪管理', '', 4, 1),
  (1905, 1009, '通勤时间', '', 5, 1),
  (1906, 1009, '周末计划', '', 6, 1),
  (1907, 1009, '断舍离', '', 7, 1),
  (1908, 1009, '独处时光', '', 8, 1),
  (2001, 1010, '陶艺入门', '', 1, 1),
  (2002, 1010, '编织围巾', '', 2, 1),
  (2003, 1010, '手工皂', '', 3, 1),
  (2004, 1010, '干花相框', '', 4, 1),
  (2005, 1010, '帆布改造', '', 5, 1),
  (2006, 1010, '皮质小物', '', 6, 1),
  (2007, 1010, '手账排版', '', 7, 1),
  (2008, 1010, '羊毛毡', '', 8, 1)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `name` = VALUES(`name`),
  `icon` = VALUES(`icon`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`);

-- 初始化词云词条
INSERT INTO `sys_word_cloud` (id, word, weight, sort_order, status) VALUES
  (2001, '小户型收纳', 5, 1, 1),
  (2002, '一人食', 4, 2, 1),
  (2003, '秋日露营', 4, 3, 1),
  (2004, '通勤穿搭', 3, 4, 1),
  (2005, '手机摄影', 3, 5, 1),
  (2006, '周末短途', 3, 6, 1),
  (2007, '手冲咖啡', 2, 7, 1),
  (2008, '情绪管理', 2, 8, 1),
  (2009, '桌面改造', 2, 9, 1),
  (2010, '胶片色调', 2, 10, 1)
ON DUPLICATE KEY UPDATE `word` = VALUES(`word`);
