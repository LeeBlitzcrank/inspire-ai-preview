-- =============================================
-- 灵感核心服务 自动建表
-- safe: CREATE IF NOT EXISTS + continue-on-error
-- =============================================

CREATE TABLE IF NOT EXISTS `inspire_main` (
  `id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL COMMENT '灵感标题',
  `img` VARCHAR(255) DEFAULT '' COMMENT '封面图',
  `tag` VARCHAR(30) NOT NULL COMMENT '分类',
  `user_id` BIGINT NOT NULL COMMENT '发布人ID',
  `status` TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
  `view_count` BIGINT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `collect_count` INT DEFAULT 0,
  `heat` INT DEFAULT 0,
  `publish_city` VARCHAR(32) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  `ext_json` JSON DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tag_deleted` (`tag`,`deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感主表';

-- 兼容已有表：添加 status 字段（continue-on-error=true 忽略重复添加）
ALTER TABLE `inspire_main` ADD COLUMN `status` TINYINT DEFAULT 0 COMMENT '0草稿 1已发布' AFTER `user_id`;

CREATE TABLE IF NOT EXISTS `inspire_content` (
  `inspire_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL COMMENT '灵感正文',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感正文附表';

-- ========== 收藏分表 collect_0 ~ collect_9 ==========
CREATE TABLE IF NOT EXISTS `collect_0`  ( `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `inspire_id` BIGINT NOT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `collect_1`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_2`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_3`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_4`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_5`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_6`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_7`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_8`  LIKE `collect_0`;
CREATE TABLE IF NOT EXISTS `collect_9`  LIKE `collect_0`;

-- ========== 点赞分表 inspire_like_0 ~ inspire_like_9 ==========
CREATE TABLE IF NOT EXISTS `inspire_like_0`  ( `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `inspire_id` BIGINT NOT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), UNIQUE KEY `uk_inspire_user` (`inspire_id`,`user_id`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `inspire_like_1`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_2`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_3`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_4`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_5`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_6`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_7`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_8`  LIKE `inspire_like_0`;
CREATE TABLE IF NOT EXISTS `inspire_like_9`  LIKE `inspire_like_0`;
