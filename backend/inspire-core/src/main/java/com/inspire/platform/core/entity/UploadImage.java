package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 图片上传元数据实体（文档第6章 sys_upload_image 表）
 * <p>
 * 仅存储图片元数据，不存储二进制图片。
 */
@TableName("sys_upload_image")
public class UploadImage {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** MinIO文件路径：upload/{userId}/{yyyyMMdd}/{UUID}.{ext} */
    private String fileKey;

    /** 原始文件名（上传时丢弃，仅记录） */
    private String originalName;

    /** CDN访问地址 */
    private String cdnUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型（jpg/png/webp） */
    private String fileType;

    /** MIME类型 */
    private String mimeType;

    /** 图片宽度（px） */
    private Integer width;

    /** 图片高度（px） */
    private Integer height;

    /** 是否私有：0公开 1私有 */
    private Integer isPrivate;

    /** 逻辑删除：0正常 1已删除 */
    private Integer isDeleted;

    /** 上传用户ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== Getters & Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String v) { this.fileKey = v; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { this.originalName = v; }
    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String v) { this.cdnUrl = v; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long v) { this.fileSize = v; }
    public String getFileType() { return fileType; }
    public void setFileType(String v) { this.fileType = v; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String v) { this.mimeType = v; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer v) { this.width = v; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer v) { this.height = v; }
    public Integer getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Integer v) { this.isPrivate = v; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer v) { this.isDeleted = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
