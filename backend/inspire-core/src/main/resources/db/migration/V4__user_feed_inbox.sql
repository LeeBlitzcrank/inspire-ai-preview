CREATE TABLE IF NOT EXISTS `user_feed` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '收件用户',
  `inspire_id` BIGINT NOT NULL,
  `author_id` BIGINT NOT NULL COMMENT '发布者',
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`user_id`,`id`),
  UNIQUE KEY `uk_user_inspire` (`user_id`,`inspire_id`),
  KEY `idx_user_time` (`user_id`,`create_time`,`id`),
  KEY `idx_user_author_time` (`user_id`,`author_id`,`create_time`,`id`),
  KEY `idx_inspire_user` (`inspire_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(`user_id`) PARTITIONS 32;
