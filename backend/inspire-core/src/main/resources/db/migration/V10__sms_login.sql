-- 文件：backend/inspire-core/src/main/resources/db/migration/V10__sms_login.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：增加手机号登录和绑定所需字段
-- INSPIRE_FILE_HEADER

ALTER TABLE `user`
  MODIFY COLUMN `email` VARCHAR(254) DEFAULT NULL COMMENT '用户邮箱（用于找回密码）',
  ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '绑定手机号' AFTER `email`,
  ADD COLUMN `phone_verified` TINYINT DEFAULT 0 COMMENT '手机号是否已验证' AFTER `phone`,
  ADD UNIQUE KEY `uk_phone` (`phone`);
