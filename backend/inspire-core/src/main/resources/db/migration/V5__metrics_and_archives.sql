--
-- 文件：backend/inspire-core/src/main/resources/db/migration/V5__metrics_and_archives.sql
-- 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
-- INSPIRE_FILE_HEADER
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
