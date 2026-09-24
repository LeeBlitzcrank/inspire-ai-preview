CREATE TABLE IF NOT EXISTS `inspire_metric` (
  `inspire_id` BIGINT NOT NULL,
  `view_count` BIGINT NOT NULL DEFAULT 0,
  `like_count` INT NOT NULL DEFAULT 0,
  `collect_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `share_count` INT NOT NULL DEFAULT 0,
  `heat` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspire_id`),
  KEY `idx_metric_heat` (`heat`),
  KEY `idx_metric_update` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵感计数投影表';

CREATE TABLE IF NOT EXISTS `user_notification_archive` LIKE `user_notification`;
CREATE TABLE IF NOT EXISTS `web_vital_metric_archive` LIKE `web_vital_metric`;
CREATE TABLE IF NOT EXISTS `ai_call_log_archive` LIKE `ai_call_log`;
CREATE TABLE IF NOT EXISTS `user_ai_history_archive` LIKE `user_ai_history`;
CREATE TABLE IF NOT EXISTS `message_archive` LIKE `message`;
