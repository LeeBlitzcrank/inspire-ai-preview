-- 文件：backend/inspire-core/src/main/resources/db/migration/V17__recommend_admin_management.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：增加推荐配置、人工推送和推送效果归因字段
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `recommend_config` (
  `config_key` VARCHAR(80) NOT NULL,
  `config_value` VARCHAR(500) NOT NULL DEFAULT '',
  `description` VARCHAR(200) NOT NULL DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐系统动态配置';

CREATE TABLE IF NOT EXISTS `recommend_push` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspire_id` BIGINT NOT NULL,
  `target_type` VARCHAR(20) NOT NULL DEFAULT 'ALL',
  `target_value` VARCHAR(100) NOT NULL DEFAULT '',
  `weight` DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `reason` VARCHAR(200) NOT NULL DEFAULT '',
  `created_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_push_status_time` (`status`, `start_time`, `end_time`),
  KEY `idx_push_inspire` (`inspire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐人工推送';

INSERT INTO `recommend_config` (`config_key`, `config_value`, `description`) VALUES
  ('vector_recall_enabled', 'true', '是否启用内容向量召回'),
  ('treatment_percent', '50', '向量召回实验组流量百分比'),
  ('push_max_per_day', '1', '同一人工推送对单用户每日最多曝光次数')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

ALTER TABLE `recommend_event`
  ADD COLUMN `push_id` BIGINT DEFAULT NULL AFTER `variant`,
  ADD KEY `idx_push_time` (`push_id`, `create_time`);
