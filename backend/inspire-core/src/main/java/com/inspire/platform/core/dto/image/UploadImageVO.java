package com.inspire.platform.core.dto.image;

/**
 * 图片上传结果 VO（文档 7.1 节返回格式）
 */
public class UploadImageVO {

    private Long id;
    private String cdnUrl;
    private String fileKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
}
