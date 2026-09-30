--
-- 文件：backend/inspire-core/src/main/resources/db/migration/V2__message_recall_audit.sql
-- 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
-- INSPIRE_FILE_HEADER
ALTER TABLE `message`
  ADD COLUMN `recalled_by` BIGINT DEFAULT NULL COMMENT '撤回操作人' AFTER `recalled_at`,
  ADD COLUMN `original_message_hash` CHAR(64) DEFAULT NULL COMMENT '撤回前内容摘要' AFTER `recalled_by`;
