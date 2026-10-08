-- 文件：backend/inspire-core/src/main/resources/db/migration/V14__support_ticket.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：保存 AI 客服转人工工单及沟通记录
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `support_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_no` VARCHAR(32) NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `contact_value` VARCHAR(200) DEFAULT '',
  `issue` VARCHAR(1000) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `priority` TINYINT NOT NULL DEFAULT 1,
  `source` VARCHAR(30) NOT NULL DEFAULT 'ai_support',
  `last_reply` VARCHAR(500) DEFAULT '',
  `assignee_id` BIGINT DEFAULT NULL,
  `access_token_hash` VARCHAR(64) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `closed_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_status_time` (`status`, `create_time`),
  KEY `idx_assignee_status` (`assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服人工工单';

CREATE TABLE IF NOT EXISTS `support_ticket_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `sender_type` VARCHAR(20) NOT NULL,
  `sender_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(2000) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ticket_time` (`ticket_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单消息';

CREATE TABLE IF NOT EXISTS `support_ticket_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `file_url` VARCHAR(1000) NOT NULL,
  `thumb_url` VARCHAR(1000) DEFAULT '',
  `file_type` VARCHAR(20) NOT NULL DEFAULT 'image',
  `original_name` VARCHAR(255) DEFAULT '',
  `duration` VARCHAR(20) DEFAULT '',
  `sort_order` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ticket_sort` (`ticket_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单附件';
