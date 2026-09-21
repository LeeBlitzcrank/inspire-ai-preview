package com.inspire.platform.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

                // 视频需要支持 Range 请求，否则 Safari 无法播放 / 拖动进度
                if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                    return ResponseEntity.ok()
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
