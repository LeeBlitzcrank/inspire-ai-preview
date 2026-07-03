package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

    @Operation(summary = "上传文件", description = "支持 jpg/png/gif/webp，最大 10MB")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件为空");
        String name = file.getOriginalFilename();
        String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new File(dir, filename));
            String url = "/uploads/" + filename;
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
}
