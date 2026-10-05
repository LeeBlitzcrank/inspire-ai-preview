CREATE TABLE IF NOT EXISTS `world_branch` (
  `id` BIGINT NOT NULL,
  `seed_id` BIGINT NOT NULL,
  `line_id` BIGINT NOT NULL,
  `owner_user_id` BIGINT DEFAULT NULL,
  `parent_branch_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(160) NOT NULL,
  `branch_type` VARCHAR(20) NOT NULL DEFAULT 'USER',
  `visibility` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_world_branch_line` (`line_id`, `status`, `update_time`),
  KEY `idx_world_branch_owner` (`owner_user_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界线具体分支';

INSERT INTO `world_branch`
(`id`, `seed_id`, `line_id`, `owner_user_id`, `parent_branch_id`, `title`, `branch_type`, `visibility`, `status`)
SELECT `id`, `seed_id`, `id`, NULL, NULL, CONCAT('公共原线 · ', `title`), 'CANON', 1, `status`
FROM `world_line`;

ALTER TABLE `world_chapter`
  ADD COLUMN `branch_id` BIGINT DEFAULT NULL AFTER `line_id`;

UPDATE `world_chapter` c
JOIN `world_branch` b ON b.`line_id` = c.`line_id` AND b.`branch_type` = 'CANON'
SET c.`branch_id` = b.`id`
WHERE c.`branch_id` IS NULL;

ALTER TABLE `world_chapter`
  MODIFY COLUMN `branch_id` BIGINT NOT NULL,
  DROP INDEX `uk_world_chapter_no`,
  ADD UNIQUE KEY `uk_world_chapter_branch_no` (`branch_id`, `chapter_no`),
  ADD KEY `idx_world_chapter_branch_time` (`branch_id`, `create_time`);

ALTER TABLE `world_vote`
  ADD COLUMN `branch_id` BIGINT DEFAULT NULL AFTER `line_id`;

UPDATE `world_vote` v
JOIN `world_branch` b ON b.`line_id` = v.`line_id` AND b.`branch_type` = 'CANON'
SET v.`branch_id` = b.`id`
WHERE v.`branch_id` IS NULL;

ALTER TABLE `world_vote`
  MODIFY COLUMN `branch_id` BIGINT NOT NULL,
  DROP INDEX `uk_world_vote_user`,
  ADD UNIQUE KEY `uk_world_vote_branch_user` (`branch_id`, `user_id`),
  ADD KEY `idx_world_vote_branch_choice` (`branch_id`, `choice_key`);

CREATE TABLE IF NOT EXISTS `world_branch_metric` (
  `branch_id` BIGINT NOT NULL,
  `chapter_count` BIGINT NOT NULL DEFAULT 0,
  `vote_continue` BIGINT NOT NULL DEFAULT 0,
  `vote_change` BIGINT NOT NULL DEFAULT 0,
  `vote_question` BIGINT NOT NULL DEFAULT 0,
  `vote_custom` BIGINT NOT NULL DEFAULT 0,
  `vote_total` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界线分支计数';

INSERT INTO `world_branch_metric` (`branch_id`, `chapter_count`, `vote_total`)
SELECT b.`id`,
       (SELECT COUNT(*) FROM `world_chapter` c WHERE c.`branch_id` = b.`id`),
       (SELECT COUNT(*) FROM `world_vote` v WHERE v.`branch_id` = b.`id`)
FROM `world_branch` b;

UPDATE `world_branch_metric` m
JOIN (
  SELECT `branch_id`,
         SUM(CASE WHEN `choice_key` = 'continue' THEN 1 ELSE 0 END) AS vote_continue,
         SUM(CASE WHEN `choice_key` = 'change' THEN 1 ELSE 0 END) AS vote_change,
         SUM(CASE WHEN `choice_key` = 'question' THEN 1 ELSE 0 END) AS vote_question,
         SUM(CASE WHEN `choice_key` NOT IN ('continue', 'change', 'question') THEN 1 ELSE 0 END) AS vote_custom
  FROM `world_vote`
  GROUP BY `branch_id`
) v ON v.`branch_id` = m.`branch_id`
SET m.`vote_continue` = v.`vote_continue`,
    m.`vote_change` = v.`vote_change`,
    m.`vote_question` = v.`vote_question`,
    m.`vote_custom` = v.`vote_custom`;

CREATE TABLE IF NOT EXISTS `world_generation_task` (
  `id` BIGINT NOT NULL,
  `task_type` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `seed_id` BIGINT DEFAULT NULL,
  `line_id` BIGINT DEFAULT NULL,
  `branch_id` BIGINT DEFAULT NULL,
  `idempotency_key` VARCHAR(128) NOT NULL,
  `progress` TINYINT NOT NULL DEFAULT 0,
  `request_json` JSON NOT NULL,
  `result_json` JSON DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_task_idem` (`user_id`, `task_type`, `idempotency_key`),
  KEY `idx_world_task_status_time` (`status`, `update_time`),
  KEY `idx_world_task_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界种子异步生成任务';
