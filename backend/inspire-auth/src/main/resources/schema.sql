-- 用户主表
-- 对应 PRD sqlDoc 中 user 表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL COMMENT '雪花用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号，唯一不可重复',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '用户头像URL',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '用户昵称',
  `city` VARCHAR(32) DEFAULT '' COMMENT '常居城市',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '信息更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展字段：性别、生日、个性签名等',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';
