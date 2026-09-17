package com.inspire.platform.core.controller;

import com.inspire.platform.common.model.UserContext;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.image.UploadImageVO;
import com.inspire.platform.core.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传 / 私有图访问接口（文档第7章）
 * <p>
 * 所有接口均需携带 JWT Token，网关鉴权后透传 X-User-Id。
 */
@Slf4j
@Tag(name = "图片上传", description = "图片上传、私有图片签名访问")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    @Operation(summary = "上传图片", description = "文档5.2节/7.1节：四层校验→重绘清洗→MinIO上传→元数据入库")
    @PostMapping("/upload")
    public Result<UploadImageVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrivate", defaultValue = "0") Integer isPrivate) {
        Long userId = UserContext.requireUserId();
        UploadImageVO vo = imageUploadService.uploadImage(file, isPrivate, userId);
        return Result.success("上传成功", vo);
    }

    @Operation(summary = "获取私有图片临时签名链接", description = "文档5.3节/7.2节：校验所有者→生成15分钟签名URL")
    @GetMapping("/private")
    public Result<Map<String, String>> getPrivateImageUrl(
            @RequestParam("fileKey") String fileKey) {
        Long userId = UserContext.requireUserId();
        String privateUrl = imageUploadService.getPrivateImageUrl(fileKey, userId);
        return Result.success(Map.of("privateUrl", privateUrl));
    }
}
