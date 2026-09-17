-- =============================================
-- 图片上传元数据表（文档第6章）
-- 仅存储元数据，不存储图片二进制
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_upload_image` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `file_key` VARCHAR(255) NOT NULL COMMENT 'MinIO文件路径（如 upload/10001/20260713/uuid.jpg）',
  `original_name` VARCHAR(255) DEFAULT '' COMMENT '原始文件名（上传时自动丢弃，仅记录日志）',
  `cdn_url` VARCHAR(512) NOT NULL COMMENT 'CDN访问地址（https://img.20sherry.com/upload/...）',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(20) DEFAULT '' COMMENT '文件类型（jpg/png/webp）',
  `mime_type` VARCHAR(64) DEFAULT '' COMMENT 'MIME类型（image/jpeg）',
  `width` INT DEFAULT 0 COMMENT '图片宽度（px）',
  `height` INT DEFAULT 0 COMMENT '图片高度（px）',
  `is_private` TINYINT DEFAULT 0 COMMENT '是否私有（0公开 / 1私有）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0正常 / 1已删除）',
  `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_key` (`file_key`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片上传元数据表（文档6.2节，仅元数据不存二进制）';
