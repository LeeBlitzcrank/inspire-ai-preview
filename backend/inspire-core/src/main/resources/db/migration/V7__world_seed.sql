--
-- 文件：backend/inspire-core/src/main/resources/db/migration/V7__world_seed.sql
-- 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
-- INSPIRE_FILE_HEADER
CREATE TABLE IF NOT EXISTS `world_seed` (
  `id` BIGINT NOT NULL,
  `source_title` VARCHAR(120) NOT NULL,
  `source_author` VARCHAR(80) DEFAULT NULL,
  `source_text` MEDIUMTEXT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `background` MEDIUMTEXT NOT NULL,
  `rules_json` JSON DEFAULT NULL,
  `characters_json` JSON DEFAULT NULL,
  `question` VARCHAR(500) DEFAULT NULL,
  `cover_image` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_world_seed_status_time` (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界种子';

CREATE TABLE IF NOT EXISTS `world_line` (
  `id` BIGINT NOT NULL,
  `seed_id` BIGINT NOT NULL,
  `title` VARCHAR(160) NOT NULL,
  `variable` VARCHAR(500) NOT NULL,
  `environment` VARCHAR(500) DEFAULT NULL,
  `character_state` VARCHAR(500) DEFAULT NULL,
  `question` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_world_line_seed` (`seed_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界线';

CREATE TABLE IF NOT EXISTS `world_chapter` (
  `id` BIGINT NOT NULL,
  `line_id` BIGINT NOT NULL,
  `chapter_no` INT NOT NULL,
  `choice_key` VARCHAR(80) DEFAULT NULL,
  `title` VARCHAR(160) NOT NULL,
  `content` MEDIUMTEXT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_chapter_no` (`line_id`, `chapter_no`),
  KEY `idx_world_chapter_time` (`line_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界线章节';

CREATE TABLE IF NOT EXISTS `world_vote` (
  `id` BIGINT NOT NULL,
  `line_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `choice_key` VARCHAR(80) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_vote_user` (`line_id`, `user_id`),
  KEY `idx_world_vote_choice` (`line_id`, `choice_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界线投票';
