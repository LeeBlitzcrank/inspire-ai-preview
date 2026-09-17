package com.inspire.platform.core.service.impl;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.config.MinioConfig;
import com.inspire.platform.core.dto.image.UploadImageVO;
import com.inspire.platform.core.entity.UploadImage;
import com.inspire.platform.core.mapper.UploadImageMapper;
import com.inspire.platform.core.service.ImageUploadService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片上传服务实现（文档第5章全部流程）
 * <p>
 * 包含：
 * — 5.1 四层安全校验（大小/扩展名/MIME/魔数+重绘）
 * — 5.2 上传流程（校验→重绘→生成路径→MinIO上传→元数据入库）
 * — 5.3 私有图片签名 URL 生成
 */
@Slf4j
@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    // ========== 文档5.1节：四层校验常量 ==========

    /** 第一层：单文件最大 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    /** 第二层：允许的图片后缀（全部小写） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** 第三层：允许的 MIME 类型 */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /** 第四层：图片魔数标识（文件头字节） */
    private static final List<byte[]> MAGIC_BYTES = Arrays.asList(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},                // JPEG
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},                        // PNG
            new byte[]{0x52, 0x49, 0x46, 0x46}                                 // WEBP (RIFF)
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final UploadImageMapper uploadImageMapper;

    /** 文档 7.1 节：CDN 域名（不含末尾斜杠） */
    @Value("${inspire.image.cdn-domain:https://img.20sherry.com}")
    private String cdnDomain;

    public ImageUploadServiceImpl(MinioClient minioClient, MinioConfig minioConfig,
                                   UploadImageMapper uploadImageMapper) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.uploadImageMapper = uploadImageMapper;
    }

    // ========================================================================
    //  5.2 上传流程
    // ========================================================================

    @Override
    @Transactional
    public UploadImageVO uploadImage(MultipartFile file, Integer isPrivate, Long userId) {
        // ---- 文档5.1节：四层安全校验 ----
        doFourLayerValidation(file);

        // ---- 文档5.1节第4步：图片重绘清洗（清除EXIF、恶意脚本） ----
        byte[] cleanedBytes = redrawImage(file);

        // ---- 解析重绘后的图片信息 ----
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        long fileSize = cleanedBytes.length;

        // ---- 文档3.2节：生成分层存储路径 ----
        String dateDir = LocalDate.now().format(DATE_FMT);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileKey = (isPrivate != null && isPrivate == 1 ? "upload/private/" : "upload/")
                + userId + "/" + dateDir + "/" + uuid + "." + ext;

        // ---- 获取图片尺寸 ----
        int[] dimensions = getImageDimensions(cleanedBytes);

        // ---- 上传清洗后的图片到 MinIO（文档5.2节第5步） ----
        uploadToMinio(fileKey, cleanedBytes, getMimeType(ext));

        // ---- 拼接 CDN 地址 ----
        String cdnUrl = cdnDomain + "/" + fileKey;

        // ---- 元数据入库（文档5.2节第7步） ----
        UploadImage record = new UploadImage();
        record.setId(generateSnowflakeId());
        record.setFileKey(fileKey);
        record.setOriginalName(originalName != null ? originalName : "");
        record.setCdnUrl(cdnUrl);
        record.setFileSize(fileSize);
        record.setFileType(ext);
        record.setMimeType(getMimeType(ext));
        record.setWidth(dimensions[0]);
        record.setHeight(dimensions[1]);
        record.setIsPrivate(isPrivate != null && isPrivate == 1 ? 1 : 0);
        record.setIsDeleted(0);
        record.setUserId(userId);
        uploadImageMapper.insert(record);

        log.info("图片上传成功: fileKey={}, size={}, userId={}, isPrivate={}",
                fileKey, fileSize, userId, isPrivate);

        // ---- 返回结果 ----
        UploadImageVO vo = new UploadImageVO();
        vo.setId(record.getId());
        vo.setCdnUrl(cdnUrl);
        vo.setFileKey(fileKey);
        return vo;
    }

    // ========================================================================
    //  5.3 私有图片签名 URL
    // ========================================================================

    @Override
    public String getPrivateImageUrl(String fileKey, Long userId) {
        // 查询图片元数据
        UploadImage image = uploadImageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UploadImage>()
                        .eq(UploadImage::getFileKey, fileKey)
                        .eq(UploadImage::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (image == null) {
            throw new BusinessException("图片不存在或已删除");
        }

        // 校验当前用户是否为图片上传者
        if (!image.getUserId().equals(userId)) {
            log.warn("私有图越权访问: fileKey={}, requester={}, owner={}",
                    fileKey, userId, image.getUserId());
            throw new BusinessException(403001, "无权限访问此图片");
        }

        // 生成 MinIO 预签名 URL，有效期 15 分钟
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucket())
                            .object(fileKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
            log.debug("私有图签名URL生成: fileKey={}, userId={}", fileKey, userId);
            return presignedUrl;
        } catch (Exception e) {
            log.error("生成签名URL失败: fileKey={}", fileKey, e);
            throw new BusinessException("生成签名链接失败，请稍后重试");
        }
    }

    // ========================================================================
    //  5.1 四层安全校验
    // ========================================================================

    /**
     * 执行四层校验（文档5.1节）
     * <p>
     * 第一层：文件大小 ≤ 5MB
     * 第二层：文件扩展名白名单
     * 第三层：MIME 类型白名单
     * 第四层：二进制魔数校验
     */
    private void doFourLayerValidation(MultipartFile file) {
        // ---- 第一层：文件大小 ----
        if (file.isEmpty() || file.getSize() == 0) {
            throw new BusinessException("上传文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        // ---- 第二层：扩展名校验 ----
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持 JPG/PNG/WebP 格式图片");
        }

        // ---- 第三层：MIME 类型校验 ----
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIME_TYPES.contains(mime.toLowerCase())) {
            throw new BusinessException("文件MIME类型不合法");
        }

        // ---- 第四层：二进制魔数校验 ----
        try {
            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header, 0, 8);
                if (read < 4) {
                    throw new BusinessException("文件内容不完整");
                }
            }
            boolean magicMatched = MAGIC_BYTES.stream().anyMatch(magic -> {
                for (int i = 0; i < magic.length; i++) {
                    if (header[i] != magic[i]) return false;
                }
                return true;
            });
            if (!magicMatched) {
                throw new BusinessException("文件格式校验失败，疑似非法文件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("魔数校验异常", e);
            throw new BusinessException("文件校验失败");
        }
    }

    // ========================================================================
    //  图片重绘清洗（文档5.1节第4步）
    // ========================================================================

    /**
     * 图片重绘清洗
     * <p>
     * 将图片解码后重新编码输出，清除以下安全隐患：
     * — EXIF 元数据（GPS、相机信息等隐私数据）
     * — 图片内嵌的恶意脚本
     * — 图片马（攻击代码隐藏在图片二进制中）
     * <p>
     * 重绘流程：读取 MultipartFile → BufferedImage → Graphics2D 绘制 →
     * 按原格式编码输出 → 返回清洗后的字节数组。
     */
    private byte[] redrawImage(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage original = ImageIO.read(is);
            if (original == null) {
                throw new BusinessException("图片解码失败，无法清洗");
            }

            int width = original.getWidth();
            int height = original.getHeight();

            // 创建新画布，逐像素绘制（清除所有嵌入数据）
            BufferedImage cleaned = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = cleaned.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(original, 0, 0, width, height, null);
            g2d.dispose();

            // 按原格式编码输出
            String formatName = extractFormat(file.getOriginalFilename());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cleaned, formatName, baos);
            return baos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("图片重绘失败", e);
            throw new BusinessException("图片处理失败");
        }
    }

    // ========================================================================
    //  MinIO 上传
    // ========================================================================

    private void uploadToMinio(String fileKey, byte[] data, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(fileKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO上传失败: fileKey={}", fileKey, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    // ========================================================================
    //  工具方法
    // ========================================================================

    /** 提取文件扩展名（小写，不含点号） */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 获取图片格式名称（ImageIO.write 参数） */
    private String extractFormat(String filename) {
        String ext = extractExtension(filename);
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    /** 根据扩展名获取 MIME 类型 */
    private String getMimeType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /** 从重绘后的字节数组读取图片宽高 */
    private int[] getImageDimensions(byte[] imageData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.warn("读取图片尺寸失败", e);
        }
        return new int[]{0, 0};
    }

    // ========================================================================
    //  雪花ID生成器
    // ========================================================================

    private static long snowflakeLastTimestamp = -1L;
    private static long snowflakeSequence = 0L;

    private static synchronized long generateSnowflakeId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < snowflakeLastTimestamp) timestamp = snowflakeLastTimestamp;
        if (timestamp == snowflakeLastTimestamp) {
            snowflakeSequence = (snowflakeSequence + 1) & 0xFFF;
            if (snowflakeSequence == 0) timestamp++;
        } else { snowflakeSequence = 0; }
        snowflakeLastTimestamp = timestamp;
        return ((timestamp - 1735689600000L) << 22) | (1L << 12) | 1L;
    }

    /** 403001 错误码常量 */
    private static final int FORBIDDEN_NO_PERMISSION = 403001;
}
