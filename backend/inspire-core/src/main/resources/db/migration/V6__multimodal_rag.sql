--
-- 文件：backend/inspire-core/src/main/resources/db/migration/V6__multimodal_rag.sql
-- 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
-- 主要职责：数据库脚本，定义表结构、索引、迁移或初始化数据
-- 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
-- INSPIRE_FILE_HEADER
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
