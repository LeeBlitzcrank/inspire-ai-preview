ALTER TABLE `admin_user`
  ADD COLUMN `totp_secret` VARCHAR(64) DEFAULT NULL COMMENT 'TOTP 密钥' AFTER `password`,
  ADD COLUMN `totp_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用 TOTP' AFTER `totp_secret`;
