-- 文件：backend/inspire-core/src/main/resources/db/migration/V11__admin_audit_log.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：记录后台写操作审计日志
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id` BIGINT NOT NULL,
  `admin_user_id` BIGINT DEFAULT NULL,
  `method` VARCHAR(10) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `request_body` TEXT,
  `response_status` INT DEFAULT 0,
  `success` TINYINT DEFAULT 0,
  `ip` VARCHAR(64) DEFAULT '',
  `user_agent` VARCHAR(512) DEFAULT '',
  `duration_ms` BIGINT DEFAULT 0,
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_time` (`admin_user_id`, `create_time`),
  KEY `idx_path_time` (`path`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作审计日志';
