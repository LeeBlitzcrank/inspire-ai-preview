--
-- 文件：backend/inspire-core/src/main/resources/db/migration/V4__user_feed_inbox.sql
-- 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
-- INSPIRE_FILE_HEADER
CREATE TABLE IF NOT EXISTS `user_feed` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '收件用户',
  `inspire_id` BIGINT NOT NULL,
  `author_id` BIGINT NOT NULL COMMENT '发布者',
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`user_id`,`id`),
  UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`),
  KEY `idx_user_time` (`user_id`,`create_time`,`id`),
  KEY `idx_user_author_time` (`user_id`,`author_id`,`create_time`,`id`),
  KEY `idx_inspire_user` (`inspire_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(`user_id`) PARTITIONS 32;
