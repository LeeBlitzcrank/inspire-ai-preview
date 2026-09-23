package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@Tag(name = "文件上传", description = "图片/视频上传")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.inspire.platform.core.service.ImageVariantService imageVariantService;

    /** 视频：单文件最大 50MB */
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024L;
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov", ".m4v");
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-m4v");

    @Value("${inspire.upload.dir:/tmp/inspire-uploads}")
    private String uploadDir;

    @Value("${inspire.upload.remote-allowed-hosts:images.unsplash.com,plus.unsplash.com,picsum.photos,fastly.picsum.photos,img.20sherry.com}")
    private String remoteAllowedHosts;

    private static final long MAX_REMOTE_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_PROXY_IMAGE_SIZE = 12 * 1024 * 1024L;

    @Operation(summary = "上传文件", description = "支持 jpg/png/gif/webp，最大 10MB")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        String name = file.getOriginalFilename();
        String rawExt = name != null && name.contains(".")
                ? name.substring(name.lastIndexOf(".")).toLowerCase() : ".jpg";
        // 视频走独立分支：不做图片重绘，仅做大小/MIME/魔数三层校验
        if (VIDEO_EXTENSIONS.contains(rawExt)) {
            return uploadVideo(file, rawExt, name);
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + rawExt;
        // 输出格式：png 保留透明通道，其余统一 jpg
        String format = ".png".equals(rawExt) ? "png" : "jpg";
        int imgType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 一次性读入内存，避免多次磁盘 IO
            byte[] sourceBytes = file.getBytes();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (img == null) {
                return Result.error("图片解析失败，请上传有效图片");
            }

            // 压缩：仅当宽度超过 1920px 时才缩放
            BufferedImage out = img;
            int maxW = 1920;
            if (img.getWidth() > maxW) {
                int newW = maxW;
                int newH = Math.max((int) (maxW * (double) img.getHeight() / img.getWidth()), 1);
                out = new BufferedImage(newW, newH, imgType);
                Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, newW, newH, null);
                g.dispose();
                log.info("图片已压缩: {}x{} -> {}x{}", img.getWidth(), img.getHeight(), newW, newH);
            }
            ImageIO.write(out, format, new File(dir, filename));

            // 缩略图（400px）直接从内存图像生成，不再二次读盘
            int tw = 400;
            int th = Math.max((int) (tw * (double) out.getHeight() / out.getWidth()), 1);
            BufferedImage thumb = new BufferedImage(tw, th, imgType);
            Graphics2D g2 = thumb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(out, 0, 0, tw, th, null);
            g2.dispose();
            ImageIO.write(thumb, format, new File(dir, "thumb_" + filename));

            // 生成 200/400/800 三档 WebP，列表和详情直接取对应尺寸。
            Map<Integer, String> variants = imageVariantService.writeLocalVariants(dir, filename, sourceBytes);
            String url = variants.getOrDefault(800, "/uploads/" + filename);
            String thumbUrl = variants.getOrDefault(400, "/uploads/thumb_" + filename);
            Map<String, String> result = new java.util.LinkedHashMap<>();
            result.put("url", url);
            result.put("thumbUrl", thumbUrl);
            result.put("url200", variants.getOrDefault(200, thumbUrl));
            result.put("url400", thumbUrl);
            result.put("url800", url);
            result.put("name", name == null ? "" : name);
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 视频上传：大小 / MIME / 魔数 三层校验，落盘后尝试用 ffmpeg 截首帧做封面。
     * 不做转码，保持原始画质，压缩/裁剪由用户在上传后按需触发。
     */
    private Result<Map<String, String>> uploadVideo(MultipartFile file, String rawExt, String originName) {
        if (file.getSize() > MAX_VIDEO_SIZE) {
            return Result.error("视频不能超过 50MB");
        }
        String mime = file.getContentType();
        if (mime != null && !mime.isBlank()
                && !VIDEO_MIME_TYPES.contains(mime.toLowerCase())
                && !"application/octet-stream".equalsIgnoreCase(mime)) {
            return Result.error("仅支持 mp4 / webm / mov 格式的视频");
        }
        try {
            byte[] head = new byte[16];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.read(head);
            }
            if (read < 12 || !isVideoMagic(head, rawExt)) {
                return Result.error("视频文件校验失败，请确认文件未损坏");
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + rawExt;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, filename);
            try (InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String thumbUrl = "";
            String poster = "thumb_" + filename + ".jpg";
            if (runFfmpeg(List.of("-y", "-ss", "0", "-i", target.getAbsolutePath(),
                    "-frames:v", "1", "-q:v", "3", new File(dir, poster).getAbsolutePath()))) {
                thumbUrl = "/uploads/" + poster;
            }
            long duration = probeDuration(target);
            log.info("视频上传成功: {} ({}MB, {}s)", filename, file.getSize() / 1024 / 1024, duration);
            return Result.success(Map.of(
                    "url", "/uploads/" + filename,
                    "thumbUrl", thumbUrl,
                    "name", originName == null ? filename : originName,
                    "type", "video",
                    "duration", String.valueOf(duration)));
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error("视频上传失败: " + e.getMessage());
        }
    }

    /** 视频魔数：mp4/mov/m4v 第 4-7 字节为 "ftyp"，webm 为 EBML 头 1A45DFA3 */
    private boolean isVideoMagic(byte[] head, String ext) {
        if (".webm".equals(ext)) {
            return (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                    && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3;
        }
        return head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
    }

    /** 执行 ffmpeg，返回是否成功（未安装 ffmpeg 时返回 false，不抛异常） */
    private boolean runFfmpeg(List<String> args) {
        try {
            List<String> cmd = new java.util.ArrayList<>();
            cmd.add("ffmpeg");
            cmd.addAll(args);
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.warn("ffmpeg 处理超时，已跳过封面生成");
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            log.warn("ffmpeg 调用失败（可能未安装）: {}", e.getMessage());
            return false;
        }
    }

    private long probeDuration(File video) {
        try {
            Process p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1", video.getAbsolutePath())
                    .redirectErrorStream(true).start();
            if (!p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return 0L;
            }
            String out = new String(p.getInputStream().readAllBytes()).trim();
            if (out.isEmpty()) return 0L;
            return Math.round(Double.parseDouble(out));
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 海报封面代理：把站外图片用本站域名转发出去。
     *
     * 各家图源的 CORS 响应在不同网络/边缘节点上并不一致，前端 canvas 直接画会时好时坏；
     * 走这个接口拿到的字节流由我们自己加 CORS 头，海报导出就稳定了。
     * 只允许白名单图源，且限制大小，避免变成开放代理。
     */
    @Operation(summary = "海报封面代理", description = "仅白名单图源，返回图片字节流并带 CORS 头")
    @GetMapping("/poster-cover")
    public ResponseEntity<byte[]> posterCover(@RequestParam("url") String urlStr) {
        try {
            URI uri = new URI(urlStr);
            if (!isAllowedRemoteUri(uri)) {
                log.warn("海报封面代理拒绝非白名单图源: {}", uri.getHost());
                return ResponseEntity.status(403).build();
            }

            HttpURLConnection conn = openRemoteConnection(uri, 8000, 15000);
            try {
                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    log.warn("海报封面代理上游返回 {}: {}", status, uri);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                long contentLength = conn.getContentLengthLong();
                if (contentLength > MAX_PROXY_IMAGE_SIZE) {
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
                }
                String contentType = conn.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
                }
                byte[] bytes;
                try (InputStream in = conn.getInputStream()) {
                    bytes = readLimited(in, MAX_PROXY_IMAGE_SIZE);
                }
                if (bytes.length == 0) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                return ResponseEntity.ok()
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Cache-Control", "public, max-age=86400")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(bytes);
            } finally {
                conn.disconnect();
            }
        } catch (IllegalArgumentException | IOException e) {
            log.warn("海报封面代理失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (Exception e) {
            log.warn("海报封面代理失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @Operation(summary = "从URL上传图片", description = "传入外部图片URL，服务端下载后存储，避免CORS")
    @PostMapping("/upload-from-url")
    public Result<Map<String, String>> uploadFromUrl(@RequestBody Map<String, String> body) {
        String urlStr = body.get("url");
        if (urlStr == null || urlStr.isBlank()) {
            return Result.error("url 参数为空");
        }
        try {
            URI uri = new URI(urlStr);
            if (!isAllowedRemoteUri(uri)) {
                return Result.error("不允许的图片来源");
            }

            String ext = ".jpg";
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (path.contains(".")) {
                String rawExt = path.substring(path.lastIndexOf(".")).toLowerCase();
                if (rawExt.matches("\\.(png|jpg|jpeg|gif|webp|bmp)")) {
                    ext = rawExt;
                }
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            HttpURLConnection conn = openRemoteConnection(uri, 10000, 30000);
            byte[] sourceBytes;
            try {
                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    return Result.error("下载图片失败，HTTP " + status);
                }
                long contentLength = conn.getContentLengthLong();
                if (contentLength > MAX_REMOTE_IMAGE_SIZE) {
                    return Result.error("远程图片不能超过 10MB");
                }
                String contentType = conn.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    return Result.error("远程地址不是图片");
                }
                try (InputStream in = conn.getInputStream()) {
                    sourceBytes = readLimited(in, MAX_REMOTE_IMAGE_SIZE);
                }
            } finally {
                conn.disconnect();
            }
            if (sourceBytes.length == 0) {
                return Result.error("远程图片内容为空");
            }
            if (".webp".equalsIgnoreCase(ext) && !isWebp(sourceBytes)) {
                return Result.error("WebP 图片内容校验失败");
            }
            File target = new File(dir, filename);
            Files.write(target.toPath(), sourceBytes);

            String urlPath = "/uploads/" + filename;
            String thumbUrl = urlPath;
            // WebP 保持自身地址；Java ImageIO 不能稳定解码 WebP。
            if (!".webp".equalsIgnoreCase(ext)) {
                try {
                    BufferedImage original = ImageIO.read(target);
                    if (original == null) {
                        target.delete();
                        return Result.error("图片解析失败");
                    }
                    int tw = 400;
                    int th = (int)(tw * (double)original.getHeight() / original.getWidth());
                    BufferedImage thumb = new BufferedImage(tw, Math.max(th, 1), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = thumb.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(original, 0, 0, tw, th, null);
                    g2d.dispose();
                    ImageIO.write(thumb, "jpg", new File(dir, "thumb_" + filename));
                    thumbUrl = "/uploads/thumb_" + filename;
                } catch (Exception e) { log.warn("缩略图生成失败: {}", e.getMessage()); }
            }
            return Result.success(Map.of("url", urlPath, "thumbUrl", thumbUrl, "name", filename));
        } catch (Exception e) {
            log.error("从URL上传图片失败: {}", e.getMessage(), e);
            return Result.error("从URL上传失败: " + e.getMessage());
        }
    }

    private HttpURLConnection openRemoteConnection(URI uri, int connectTimeout, int readTimeout)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    private boolean isAllowedRemoteUri(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            return false;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (!getAllowedRemoteHosts().contains(host)) {
            return false;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    log.warn("远程图片地址解析到私网 IP: host={}, ip={}", host, address.getHostAddress());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("远程图片域名解析失败: {}", host);
            return false;
        }
    }

    private Set<String> getAllowedRemoteHosts() {
        return Arrays.stream(remoteAllowedHosts.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(host -> !host.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    private byte[] readLimited(InputStream in, long maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("远程内容过大");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
