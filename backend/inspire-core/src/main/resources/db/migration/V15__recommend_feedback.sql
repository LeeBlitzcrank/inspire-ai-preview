-- 文件：backend/inspire-core/src/main/resources/db/migration/V15__recommend_feedback.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：记录推荐曝光、点击和正负反馈，用于个性化推荐
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `recommend_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `inspire_id` BIGINT NOT NULL,
  `event_type` VARCHAR(20) NOT NULL,
  `reason_code` VARCHAR(40) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_user_inspire_event` (`user_id`, `inspire_id`, `event_type`),
  KEY `idx_inspire_time` (`inspire_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐行为与反馈';
