-- 文件：backend/inspire-core/src/main/resources/db/migration/V12__support_rag_state.sql
-- 所属模块：数据库初始化和结构说明
-- 主要职责：记录项目客服知识库索引状态
-- INSPIRE_FILE_HEADER

CREATE TABLE IF NOT EXISTS `support_rag_state` (
  `id` TINYINT NOT NULL,
  `fingerprint` VARCHAR(64) DEFAULT NULL,
  `document_count` INT NOT NULL DEFAULT 0,
  `chunk_count` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'idle',
  `error_message` VARCHAR(500) DEFAULT NULL,
  `indexed_at` DATETIME DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目客服RAG索引状态';
