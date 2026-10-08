-- 文件：backend/inspire-core/src/main/resources/db/migration/V13__login_log.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：记录登录、刷新令牌等认证操作的审计日志
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `login_log` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL DEFAULT 0,
  `username` VARCHAR(50) NOT NULL DEFAULT '',
  `ip` VARCHAR(64) NOT NULL DEFAULT '',
  `device_id` VARCHAR(128) NOT NULL DEFAULT '',
  `user_agent` VARCHAR(512) NOT NULL DEFAULT '',
  `login_type` VARCHAR(20) NOT NULL DEFAULT '',
  `result` TINYINT NOT NULL DEFAULT 0,
  `fail_reason` VARCHAR(255) NOT NULL DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_username_time` (`username`, `create_time`),
  KEY `idx_ip_time` (`ip`, `create_time`),
  KEY `idx_result_time` (`result`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录操作日志';
