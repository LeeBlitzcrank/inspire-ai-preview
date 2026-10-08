/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/controller/VideoController.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.VideoCompressRequest;
import com.inspire.platform.core.dto.VideoProbeRequest;
import com.inspire.platform.core.dto.VideoTrimRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;

/**
 * 视频后期处理：探测信息 / 压缩 / 裁剪。
 *
 * <p>依赖容器内已安装 ffmpeg/ffprobe（见 deploy/docker/Dockerfile.local）。
 * 所有操作都是「读原文件 → 生成新文件」，不会覆盖用户已上传的原片，
 * 前端可以选择保留原片还是用处理后的结果替换。
 */
@Tag(name = "视频处理", description = "视频压缩、裁剪、信息探测")
@RestController
@RequestMapping("/file/video")
@Slf4j
public class VideoController {

    @Value("${inspire.upload.dir:/tmp/inspire-uploads}")
    private String uploadDir;

    /** 把 /uploads/xxx.mp4 解析成磁盘文件，并阻止路径穿越 */
    private File resolve(String url) {
        if (url == null || url.isBlank()) return null;
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) return null;
        File f = new File(uploadDir, name);
        return f.exists() && f.isFile() ? f : null;
    }

    /**
     * 「只保留最终版」：处理成功后删除原片及其封面，避免磁盘被过程文件堆满。
     * 仅在调用方显式传 keepOriginal=false 时执行。
     */
    private void deleteOriginal(File video, boolean keepOriginal) {
        if (keepOriginal) return;
        String name = video.getName().toLowerCase();
        boolean isVideo = name.endsWith(".mp4") || name.endsWith(".webm")
                || name.endsWith(".mov") || name.endsWith(".m4v");
        if (!isVideo) return;
        try {
            java.nio.file.Files.deleteIfExists(video.toPath());
            java.nio.file.Files.deleteIfExists(new File(uploadDir, "thumb_" + video.getName() + ".jpg").toPath());
            log.info("已删除原片（只保留最终版）: {}", video.getName());
        } catch (Exception e) {
            log.warn("删除原片失败: {}", e.getMessage());
        }
    }

    private boolean keepOriginal(Boolean flag) {
        return flag == null || Boolean.TRUE.equals(flag);
    }

    private String ffprobe(File video, String entries, String stream) {
        List<String> cmd = new ArrayList<>(List.of("ffprobe", "-v", "error"));
        if (stream != null) {
            cmd.addAll(List.of("-select_streams", stream));
        }
        cmd.addAll(List.of("-show_entries", entries, "-of", "default=noprint_wrappers=1:nokey=1",
                video.getAbsolutePath()));
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return out;
        } catch (Exception e) {
            log.warn("ffprobe 调用失败: {}", e.getMessage());
            return "";
        }
    }

    private boolean ffmpeg(List<String> args, File out) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.addAll(args);
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String logText = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0) {
                log.warn("ffmpeg 执行失败({}): {}", code, logText.length() > 500 ? logText.substring(0, 500) : logText);
            }
            return code == 0 && out.exists() && out.length() > 0;
        } catch (Exception e) {
            log.warn("ffmpeg 调用失败（可能未安装）: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> describe(File file) {
        Map<String, Object> data = new HashMap<>();
        data.put("url", "/uploads/" + file.getName());
        data.put("name", file.getName());
        data.put("size", file.length());
        data.put("sizeText", String.format("%.1fMB", file.length() / 1024.0 / 1024.0));
        String duration = ffprobe(file, "format=duration", null);
        try {
            data.put("duration", duration.isEmpty() ? 0 : Math.round(Double.parseDouble(duration)));
        } catch (Exception e) {
            data.put("duration", 0);
        }
        return data;
    }

    @Operation(summary = "探测视频信息", description = "返回时长、体积、分辨率，用于前端展示与裁剪范围")
    @PostMapping("/probe")
    public Result<Map<String, Object>> probe(@Valid @RequestBody VideoProbeRequest body) {
        File video = resolve(body.getUrl());
        if (video == null) return Result.error("视频不存在");
        Map<String, Object> data = describe(video);
        String wh = ffprobe(video, "stream=width,height", "v:0").replace("\n", "x");
        data.put("resolution", wh);
        return Result.success(data);
    }

    @Operation(summary = "压缩视频", description = "H.264 + CRF 质量压缩，体积通常可降到原来的 30%-60%")
    @PostMapping("/compress")
    public Result<Map<String, Object>> compress(@Valid @RequestBody VideoCompressRequest body) {
        File video = resolve(body.getUrl());
        if (video == null) return Result.error("视频不存在");
        int crf = body.getCrf() == null ? 28 : body.getCrf();

        String name = UUID.randomUUID().toString().replace("-", "") + ".mp4";
        File out = new File(uploadDir, name);
        List<String> args = new ArrayList<>(List.of("-y", "-i", video.getAbsolutePath(),
                "-vcodec", "libx264", "-crf", String.valueOf(crf), "-preset", "veryfast"));
        // 高于 720p 才缩放，避免把小视频放大
        int height = 0;
        try {
            String h = ffprobe(video, "stream=height", "v:0").lines().findFirst().orElse("");
            height = h.isBlank() ? 0 : Integer.parseInt(h.trim());
        } catch (Exception ignored) {}
        if (height > 720) {
            args.addAll(List.of("-vf", "scale=-2:720"));
        }
        args.addAll(List.of("-acodec", "aac", "-b:a", "128k", "-movflags", "+faststart",
                out.getAbsolutePath()));
        boolean ok = ffmpeg(args, out);
        if (!ok) return Result.error("压缩失败，请确认服务端已安装 ffmpeg");
        deleteOriginal(video, keepOriginal(body.getKeepOriginal()));
        return Result.success(describe(out));
    }

    @Operation(summary = "裁剪视频", description = "按起止时间截取片段（-c copy 快速截取，不重新编码）")
    @PostMapping("/trim")
    public Result<Map<String, Object>> trim(@Valid @RequestBody VideoTrimRequest body) {
        File video = resolve(body.getUrl());
        if (video == null) return Result.error("视频不存在");
        double start = body.getStart() == null ? 0D : body.getStart();
        double duration = body.getDuration();
        if (duration <= 0) return Result.error("请选择需要保留的时长");

        String name = UUID.randomUUID().toString().replace("-", "") + ".mp4";
        File out = new File(uploadDir, name);
        boolean ok = ffmpeg(List.of("-y", "-ss", String.valueOf(start), "-i", video.getAbsolutePath(),
                "-t", String.valueOf(duration), "-c", "copy", "-movflags", "+faststart",
                out.getAbsolutePath()), out);
        if (!ok) return Result.error("裁剪失败，请确认服务端已安装 ffmpeg");
        deleteOriginal(video, keepOriginal(body.getKeepOriginal()));
        return Result.success(describe(out));
    }
}
