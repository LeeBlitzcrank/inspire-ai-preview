package com.inspire.platform.core.service;

import com.inspire.platform.core.dto.image.UploadImageVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传服务（文档第5章）
 * <p>
 * 包含上传四层安全校验、图片重绘清洗、MinIO上传、私有图签名。
 */
public interface ImageUploadService {

    /**
     * 上传图片（文档 5.2 节完整流程）
     *
     * @param file      前端上传的 MultipartFile
     * @param isPrivate 0=公开 1=私有
     * @param userId    上传者用户ID
     * @return 上传结果 VO
     */
    UploadImageVO uploadImage(MultipartFile file, Integer isPrivate, Long userId);

    /**
     * 获取私有图片临时签名 URL（文档 5.3 节）
     *
     * @param fileKey  MinIO 文件路径
     * @param userId   当前登录用户ID（校验图片所有者）
     * @return 15分钟有效期的临时访问链接
     */
    String getPrivateImageUrl(String fileKey, Long userId);
}
