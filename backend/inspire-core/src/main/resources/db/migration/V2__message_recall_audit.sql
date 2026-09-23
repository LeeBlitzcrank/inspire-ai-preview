ALTER TABLE `message`
  ADD COLUMN `recalled_by` BIGINT DEFAULT NULL COMMENT '撤回操作人' AFTER `recalled_at`,
  ADD COLUMN `original_message_hash` CHAR(64) DEFAULT NULL COMMENT '撤回前内容摘要' AFTER `recalled_by`;
