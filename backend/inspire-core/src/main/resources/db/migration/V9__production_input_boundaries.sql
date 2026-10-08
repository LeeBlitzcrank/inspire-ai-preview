-- 文件：backend/inspire-core/src/main/resources/db/migration/V9__production_input_boundaries.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：这里只调整存储边界，业务合法性仍由 DTO 和 Service 校验。
-- INSPIRE_FILE_HEADER

ALTER TABLE `user`
  MODIFY COLUMN `email` VARCHAR(254) DEFAULT '' COMMENT '用户邮箱（必填，用于找回密码）',
  MODIFY COLUMN `avatar` VARCHAR(2048) DEFAULT '' COMMENT '用户头像URL';

ALTER TABLE `password_reset`
  MODIFY COLUMN `email` VARCHAR(254) NOT NULL COMMENT '接收重置邮件的邮箱';

ALTER TABLE `inspire_main`
  MODIFY COLUMN `img` VARCHAR(2048) DEFAULT '' COMMENT '封面图';

ALTER TABLE `inspire_version`
  MODIFY COLUMN `img` VARCHAR(2048) DEFAULT '' COMMENT '封面图';

ALTER TABLE `inspire_series`
  MODIFY COLUMN `cover` VARCHAR(2048) DEFAULT '' COMMENT '系列封面';

ALTER TABLE `inspire_comment`
  MODIFY COLUMN `avatar` VARCHAR(2048) DEFAULT '' COMMENT '评论者头像';
