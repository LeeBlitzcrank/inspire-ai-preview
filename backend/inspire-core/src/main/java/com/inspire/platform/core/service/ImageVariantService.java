package com.inspire.platform.core.service;

import com.inspire.platform.core.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 上传时生成多尺寸 WebP，避免列表页加载原图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageVariantService {

    private static final int[] WIDTHS = {200, 400, 800};

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${inspire.image.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${inspire.image.cdn-domain:https://img.20sherry.com}")
    private String cdnDomain;

    public Map<Integer, byte[]> generateWebpVariants(byte[] source) {
        Map<Integer, byte[]> variants = new LinkedHashMap<>();
        for (int width : WIDTHS) {
            try {
                byte[] bytes = resizeToWebp(source, width);
                if (bytes.length > 0) variants.put(width, bytes);
            } catch (Exception e) {
                log.warn("WebP变体生成失败 width={}: {}", width, e.getMessage());
            }
        }
        return variants;
    }

    public Map<Integer, String> uploadMinioVariants(String sourceKey, byte[] source) {
        Map<Integer, String> urls = new LinkedHashMap<>();
        Map<Integer, byte[]> variants = generateWebpVariants(source);
        for (Map.Entry<Integer, byte[]> entry : variants.entrySet()) {
            String key = variantKey(sourceKey, entry.getKey());
            try {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(key)
                        .stream(new ByteArrayInputStream(entry.getValue()), entry.getValue().length, -1)
                        .contentType("image/webp")
                        .build());
                urls.put(entry.getKey(), cdnDomain.replaceAll("/+$", "") + "/" + key);
            } catch (Exception e) {
                log.warn("WebP变体上传失败 key={}: {}", key, e.getMessage());
            }
        }
        return urls;
    }

    /**
     * 确保 MinIO 中已有三档 WebP。全部存在时只做 stat，不重复编码。
     */
    public Map<Integer, String> ensureMinioVariants(String sourceKey) {
        Map<Integer, String> existing = new LinkedHashMap<>();
        boolean complete = true;
        for (int width : WIDTHS) {
            String key = variantKey(sourceKey, width);
            try {
                minioClient.statObject(io.minio.StatObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(key)
                        .build());
                existing.put(width, cdnDomain.replaceAll("/+$", "") + "/" + key);
            } catch (Exception e) {
                complete = false;
                break;
            }
        }
        if (complete) return existing;

        try (java.io.InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucket())
                .object(sourceKey)
                .build())) {
            return uploadMinioVariants(sourceKey, in.readAllBytes());
        } catch (Exception e) {
            log.warn("读取原图生成WebP失败 key={}: {}", sourceKey, e.getMessage());
            return Map.of();
        }
    }

    public Map<Integer, String> writeLocalVariants(File dir, String filename, byte[] source) {
        Map<Integer, String> urls = new LinkedHashMap<>();
        Map<Integer, byte[]> variants = generateWebpVariants(source);
        String base = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        for (Map.Entry<Integer, byte[]> entry : variants.entrySet()) {
            String name = base + "_w" + entry.getKey() + ".webp";
            try {
                Files.write(new File(dir, name).toPath(), entry.getValue());
                urls.put(entry.getKey(), "/uploads/" + name);
            } catch (Exception e) {
                log.warn("WebP变体落盘失败 name={}: {}", name, e.getMessage());
            }
        }
        return urls;
    }

    public String variantKey(String sourceKey, int width) {
        int dot = sourceKey.lastIndexOf('.');
        String base = dot > 0 ? sourceKey.substring(0, dot) : sourceKey;
        return base + "_w" + width + ".webp";
    }

    private byte[] resizeToWebp(byte[] source, int width) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegBin,
                "-hide_banner", "-loglevel", "error",
                "-i", "pipe:0",
                "-vf", "scale=min(" + width + "\\,iw):-2",
                "-frames:v", "1",
                "-c:v", "libwebp",
                "-quality", "78",
                "-preset", "picture",
                "-f", "webp",
                "pipe:1"
        );
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();

        CompletableFuture<byte[]> outputFuture =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return process.getInputStream().readAllBytes();
                    } catch (Exception e) {
                        return new byte[0];
                    }
                });

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(source);
        }
        boolean finished = process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg timeout");
        }
        byte[] result = outputFuture.join();
        if (process.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg exit " + process.exitValue());
        }
        return result;
    }
}
