CREATE TABLE IF NOT EXISTS `rag_index_state` (
  `inspire_id` BIGINT NOT NULL,
  `content_hash` CHAR(64) DEFAULT NULL,
  `chunk_count` INT NOT NULL DEFAULT 0,
  `source_update_time` DATETIME DEFAULT NULL,
  `indexed_at` DATETIME DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`inspire_id`),
  KEY `idx_rag_indexed_at` (`indexed_at`),
  KEY `idx_rag_source_update` (`source_update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG索引状态';

CREATE TABLE IF NOT EXISTS `rag_query_log` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `query_text` VARCHAR(500) NOT NULL,
  `mode` VARCHAR(20) NOT NULL,
  `top_k` INT NOT NULL DEFAULT 0,
  `hit_count` INT NOT NULL DEFAULT 0,
  `latency_ms` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rag_query_user_time` (`user_id`,`create_time`),
  KEY `idx_rag_query_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG查询日志';
