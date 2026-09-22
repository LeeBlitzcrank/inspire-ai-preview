package com.inspire.platform.core.controller;

import com.inspire.platform.core.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 站内取图接口：把 MinIO 里的对象通过本站域名输出。
 *
 * <p>为什么不直接用 img.20sherry.com：那个域名依赖 Cloudflare Tunnel + 本地 Nginx，
 * 一旦链路不通图片就整体挂掉（实测过 502），而且本地开发也拿不到图。
 * 走这个接口本地和线上行为一致，且可用已有的 thumbnailator 按 ?w= 出缩略图。
 *
 * <p>路径挂在已有的 /api/file/** 路由下，避免新增网关路由。
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class MinioImageController {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final int MAX_WIDTH = 2000;

    @GetMapping("/view")
    public ResponseEntity<byte[]> view(@RequestParam("key") String key,
                                       @RequestParam(value = "w", required = false) Integer width) {
        if (!isSafeKey(key)) {
            return ResponseEntity.badRequest().build();
        }
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucket())
                .object(key)
                .build())) {

            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            if (width != null && width > 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Thumbnails.of(new ByteArrayInputStream(bytes))
                        .width(Math.min(width, MAX_WIDTH))
                        .outputFormat("jpg")
                        .outputQuality(0.82)
                        .toOutputStream(out);
                bytes = out.toByteArray();
            }
            return ResponseEntity.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Cache-Control", "public, max-age=2592000")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(bytes);
        } catch (Exception e) {
            log.warn("取图失败 key={}: {}", key, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /** 只允许 upload/ 下的对象，且挡住路径穿越 */
    private boolean isSafeKey(String key) {
        return key != null
                && key.startsWith("upload/")
                && key.length() < 300
                && !key.contains("..")
                && !key.contains("//")
                && !key.contains("\\");
    }
}
