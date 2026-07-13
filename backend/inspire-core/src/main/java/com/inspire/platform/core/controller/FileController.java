package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

@Tag(name = "文件上传", description = "图片/视频上传")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Value("${inspire.upload.dir:/tmp/inspire-uploads}")
    private String uploadDir;

    /** 创建信任所有证书的 SSL 上下文（用于开发环境外部 HTTPS 图片下载） */
    private static SSLSocketFactory trustAllSslFactory;

    private static synchronized SSLSocketFactory getTrustAllSslFactory() {
        if (trustAllSslFactory != null) {
            return trustAllSslFactory;
        }
        try {
            TrustManager[] trustAll = new TrustManager[]{ new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            trustAllSslFactory = ctx.getSocketFactory();
        } catch (Exception e) {
            log.warn("创建信任所有证书的 SSL 上下文失败", e);
        }
        return trustAllSslFactory;
    }

    @Operation(summary = "上传文件", description = "支持 jpg/png/gif/webp，最大 10MB")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        String name = file.getOriginalFilename();
        String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file.transferTo(new File(dir, filename));
            String url = "/uploads/" + filename;
            // 图片压缩：限制最大宽度 1920px
            try {
                BufferedImage img = ImageIO.read(new File(dir, filename));
                int maxW = 1920;
                if (img.getWidth() > maxW) {
                    int newW = maxW;
                    int newH = (int)(maxW * (double)img.getHeight() / img.getWidth());
                    BufferedImage resized = new BufferedImage(newW, Math.max(newH, 1), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = resized.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(img, 0, 0, newW, newH, null);
                    g2d.dispose();
                    ImageIO.write(resized, "jpg", new File(dir, filename));
                    log.info("图片已压缩: {}x{} -> {}x{}", img.getWidth(), img.getHeight(), newW, newH);
                }
            } catch (Exception e) { log.warn("图片压缩失败(非致命): {}", e.getMessage()); }
            // 生成缩略图（400px宽）
            try {
                BufferedImage original = ImageIO.read(new File(dir, filename));
                int tw = 400;
                int th = (int)(tw * (double)original.getHeight() / original.getWidth());
                BufferedImage thumb = new BufferedImage(tw, Math.max(th, 1), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = thumb.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(original, 0, 0, tw, th, null);
                g2d.dispose();
                ImageIO.write(thumb, "jpg", new File(dir, "thumb_" + filename));
            } catch (Exception e) { log.warn("缩略图生成失败: {}", e.getMessage()); }
            return Result.success(Map.of("url", url, "thumbUrl", "/uploads/thumb_" + filename, "name", name));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "从URL上传图片", description = "传入外部图片URL，服务端下载后存储，避免CORS")
    @PostMapping("/upload-from-url")
    public Result<Map<String, String>> uploadFromUrl(@RequestBody Map<String, String> body) {
        String urlStr = body.get("url");
        if (urlStr == null || urlStr.isBlank()) {
            return Result.error("url 参数为空");
        }
        String ext = ".jpg";
        if (urlStr.contains(".")) {
            String rawExt = urlStr.substring(urlStr.lastIndexOf("."));
            if (rawExt.contains("?")) {
                rawExt = rawExt.substring(0, rawExt.indexOf("?"));
            }
            if (rawExt.matches("\\.(png|jpg|jpeg|gif|webp|bmp)")) {
                ext = rawExt;
            }
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 如果是 HTTPS 且外部证书不受信任，使用信任所有证书的 SSLSocketFactory
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                SSLSocketFactory ssf = getTrustAllSslFactory();
                if (ssf != null && conn instanceof HttpsURLConnection) {
                    HttpsURLConnection hconn = (HttpsURLConnection) conn;
                    hconn.setSSLSocketFactory(ssf);
                    hconn.setHostnameVerifier((hostname, session) -> true);
                }
            }

            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return Result.error("下载图片失败，HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream()) {
                File target = new File(dir, filename);
                java.nio.file.Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            conn.disconnect();
            String urlPath = "/uploads/" + filename;
            // 生成缩略图
            try {
                BufferedImage original = ImageIO.read(new File(dir, filename));
                int tw = 400;
                int th = (int)(tw * (double)original.getHeight() / original.getWidth());
                BufferedImage thumb = new BufferedImage(tw, Math.max(th, 1), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = thumb.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(original, 0, 0, tw, th, null);
                g2d.dispose();
                ImageIO.write(thumb, "jpg", new File(dir, "thumb_" + filename));
            } catch (Exception e) { log.warn("缩略图生成失败: {}", e.getMessage()); }
            return Result.success(Map.of("url", urlPath, "thumbUrl", "/uploads/thumb_" + filename, "name", filename));
        } catch (Exception e) {
            log.error("从URL上传图片失败: {}", e.getMessage(), e);
            return Result.error("从URL上传失败: " + e.getMessage());
        }
    }
}
