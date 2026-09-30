/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/controller/FileServeController.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
@RequestMapping("/uploads")
public class FileServeController {

    @Value("${inspire.upload.dir:/tmp/inspire-uploads}")
    private String uploadDir;

    @GetMapping("/{filename}")
    public ResponseEntity<?> serve(@PathVariable String filename,
                                   @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        try {
            Path file = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                MediaType mediaType = MediaType.parseMediaType(
                        contentType != null ? contentType : "application/octet-stream");
                long length = resource.contentLength();
                long lastModified = Files.getLastModifiedTime(file).toMillis();

                // 视频需要支持 Range 请求，否则 Safari 无法播放 / 拖动进度
                if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                            .lastModified(lastModified)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .contentType(mediaType)
                            .contentLength(length)
                            .body(resource);
                }
                HttpRange range = HttpRange.parseRanges(rangeHeader).get(0);
                long start = range.getRangeStart(length);
                long end = range.getRangeEnd(length);
                // 单次最多返回 4MB，视频播放器会按需继续请求后续分片
                long maxChunk = 4 * 1024 * 1024L;
                if (end - start + 1 > maxChunk) {
                    end = start + maxChunk - 1;
                }
                byte[] chunk = new byte[(int) (end - start + 1)];
                try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                    raf.seek(start);
                    raf.readFully(chunk);
                }
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .lastModified(lastModified)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentType(mediaType)
                        .contentLength(chunk.length)
                        .body(chunk);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }
}
