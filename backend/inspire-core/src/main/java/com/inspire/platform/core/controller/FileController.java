package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.UUID;

@Tag(name = "文件上传", description = "图片/视频上传")
@RestController
@RequestMapping("/file")
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
            return Result.success(Map.of("url", url, "name", name));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
