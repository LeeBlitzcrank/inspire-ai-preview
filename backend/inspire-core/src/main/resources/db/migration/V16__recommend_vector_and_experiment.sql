-- 文件：backend/inspire-core/src/main/resources/db/migration/V16__recommend_vector_and_experiment.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：补充推荐向量同步状态、A/B 实验和停留时长字段
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `recommend_vector_state` (
  `inspire_id` BIGINT NOT NULL,
  `content_hash` VARCHAR(64) DEFAULT NULL,
  `source_update_time` DATETIME DEFAULT NULL,
  `indexed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspire_id`),
  KEY `idx_vector_indexed` (`indexed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐内容向量索引状态';

ALTER TABLE `recommend_event`
  ADD COLUMN `experiment_id` VARCHAR(40) NOT NULL DEFAULT '' AFTER `reason_code`,
  ADD COLUMN `variant` VARCHAR(20) NOT NULL DEFAULT '' AFTER `experiment_id`,
  ADD COLUMN `duration_ms` INT NOT NULL DEFAULT 0 AFTER `variant`;
