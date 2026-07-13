-- =============================================
-- 自动建表脚本（safe: IF NOT EXISTS + continue-on-error）
-- 数据源使用 inspire_ai_preview（与 application-dev.yml 一致）
-- =============================================

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL COMMENT '雪花用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，唯一不可重复',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `email` VARCHAR(100) DEFAULT '' COMMENT '用户邮箱（必填，用于找回密码）',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '用户头像URL',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '用户昵称',
  `city` VARCHAR(32) DEFAULT '' COMMENT '常居城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展字段：性别、生日、个性签名等',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

-- 双Token认证体系新增：角色字段（admin/core/user），状态字段（1正常 0冻结）
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色: admin/core/user' AFTER `nickname`;
ALTER TABLE `user` ADD COLUMN `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常 0冻结' AFTER `deleted`;

-- 兼容已有表：添加 email 字段（字段已存在时 spring.sql.init.continue-on-error=true 忽略错误）
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100) DEFAULT '' COMMENT '用户邮箱' AFTER `password`;
ALTER TABLE `user` ADD UNIQUE INDEX `uk_email` (`email`);

-- 密码重置令牌表
CREATE TABLE IF NOT EXISTS `password_reset` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `email` VARCHAR(100) NOT NULL COMMENT '接收重置邮件的邮箱',
  `token` VARCHAR(64) NOT NULL COMMENT '重置令牌（UUID）',
  `expiry_time` DATETIME NOT NULL COMMENT '令牌过期时间',
  `used` TINYINT DEFAULT 0 COMMENT '是否已使用 0未使用 1已使用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';

-- =============================================
-- 登录操作日志表（文档 6.2 节：记录IP、设备、时间、操作结果）
-- =============================================
CREATE TABLE IF NOT EXISTS `login_log` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `ip` VARCHAR(64) DEFAULT '' COMMENT '客户端IP',
  `device_id` VARCHAR(64) DEFAULT '' COMMENT '设备标识',
  `user_agent` VARCHAR(500) DEFAULT '' COMMENT '浏览器/设备UA',
  `login_type` VARCHAR(20) DEFAULT 'password' COMMENT '登录方式: password/refresh',
  `result` TINYINT DEFAULT 1 COMMENT '结果: 1成功 0失败',
  `fail_reason` VARCHAR(200) DEFAULT '' COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
