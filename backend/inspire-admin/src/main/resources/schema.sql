CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` INT AUTO_INCREMENT NOT NULL,
  `username` VARCHAR(50) NOT NULL COMMENT '管理员账号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '管理员昵称',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS `admin_config` (
  `id` INT AUTO_INCREMENT NOT NULL,
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置key',
  `config_value` VARCHAR(500) DEFAULT '' COMMENT '配置值',
  `desc` VARCHAR(120) DEFAULT '' COMMENT '配置说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营配置表';

INSERT IGNORE INTO admin_config(config_key,config_value,`desc`) VALUES
('recommend.hot_weight','0.6','同城热点权重'),
('recommend.interest_weight','0.4','用户兴趣权重'),
('map_tag_mapping','{"美食":"炸鸡店,火锅店","运动":"健身房","电影":"电影院","穿搭":"服装店","文案":"文创店"}','灵感分类对应地图检索关键词');
