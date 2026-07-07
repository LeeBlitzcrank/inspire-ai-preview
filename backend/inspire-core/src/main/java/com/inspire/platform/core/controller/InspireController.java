package com.inspire.platform.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.InspireMain;
import com.inspire.platform.core.entity.CollectFolder;
import com.inspire.platform.core.service.InspireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Tag(name = "灵感核心", description = "灵感列表、详情、创建、收藏、点赞")
@RestController
@RequestMapping("/inspire")
@Slf4j
@RequiredArgsConstructor
public class InspireController {

    private final InspireService inspireService;

    @Value("${inspire.unsplash.access-key:}")
    private String unsplashAccessKey;

    private static final java.net.http.HttpClient HTTP_CLIENT = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .build();

    @PostConstruct
    public void init() {
        boolean hasKey = unsplashAccessKey != null && !unsplashAccessKey.isBlank();
        log.info("Unsplash Access Key {}: prefix={}", hasKey ? "已配置" : "未配置",
            hasKey ? unsplashAccessKey.substring(0, Math.min(10, unsplashAccessKey.length())) + "..." : "无");
    }

    @Operation(summary = "公开灵感列表", description = "分页+分类筛选，支持按时间/热度排序")
    @GetMapping("/public/list")
    public Result<List<InspireVO>> listPublic(InspirePageQuery query,
            @RequestHeader(value = "X-Inspire-UserId", required = false) Long loginUserId) {
        return Result.success(inspireService.listPublic(query, loginUserId));
    }

    @Operation(summary = "灵感详情", description = "返回完整信息，含正文、收藏/点赞状态，自动增加浏览量")
    @GetMapping("/public/{id}")
    public Result<InspireVO> detail(@PathVariable Long id,
            @RequestHeader(value = "X-Inspire-UserId", required = false) Long loginUserId) {
        return Result.success(inspireService.getDetail(id, loginUserId));
    }

    @Operation(summary = "我的发布", description = "当前用户已公开发布的灵感列表")
    @GetMapping("/my")
    public Result<PageResult<InspireVO>> myPublished(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(inspireService.listMyPublished(userId, page, size));
    }

    @Operation(summary = "我的草稿", description = "当前用户未发布的草稿列表")
    @GetMapping("/my/drafts")
    public Result<PageResult<InspireVO>> myDrafts(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(inspireService.listMyDrafts(userId, page, size));
    }

    @Operation(summary = "我的收藏", description = "当前用户收藏的灵感列表")
    @GetMapping("/my/collects")
    public Result<PageResult<InspireVO>> myCollects(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(inspireService.listMyCollects(userId, page, size));
    }

    @Operation(summary = "创建灵感", description = "status=0保存草稿，status=1直接发布，命中敏感词自动设为待审核")
    @PostMapping
    public Result<InspireMain> create(
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @Valid @RequestBody InspireCreateRequest req) {
        InspireMain result = inspireService.create(req, userId);
        String msg = result.getStatus() == 2 ? "内容已提交审核，请等待管理员审核通过后发布" : "创建成功";
        return Result.success(msg, result);
    }

    @Operation(summary = "修改灵感", description = "只传需要修改的字段即可")
    @PutMapping("/{id}")
    public Result<InspireMain> update(@PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId,
            @RequestBody InspireUpdateRequest req) {
        return Result.success("修改成功", inspireService.update(id, req, userId));
    }

    @Operation(summary = "删除灵感", description = "逻辑删除，仅限本人")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.deleteById(id, userId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "收藏灵感", description = "不可重复收藏，分表 user_id%10 路由")
    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.collect(userId, inspireId);
        return Result.success("收藏成功", null);
    }

    @Operation(summary = "取消收藏", description = "取消后收藏数-1")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.uncollect(userId, inspireId);
        return Result.success("取消收藏", null);
    }

    @Operation(summary = "点赞灵感", description = "不可重复点赞，分表 inspire_id%10 路由")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.like(userId, inspireId);
        return Result.success("点赞成功", null);
    }

    @Operation(summary = "推荐灵感", description = "基于热度的推荐列表（未来可按用户偏好个性化）")
    @GetMapping("/public/recommend")
    public Result<List<InspireVO>> recommend(
            @RequestHeader(value = "X-Inspire-UserId", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(inspireService.recommend(userId, page, size));
    }

    @Operation(summary = "取消点赞", description = "取消后点赞数-1")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.unlike(userId, inspireId);
        return Result.success("取消点赞", null);
    }

    @Operation(summary = "分享灵感", description = "记录分享行为，分享数+1")
    @PostMapping("/{id}/share")
    public Result<Void> share(@PathVariable("id") Long inspireId,
            @Parameter(hidden = true) @RequestHeader("X-Inspire-UserId") Long userId) {
        inspireService.share(userId, inspireId);
        return Result.success("分享成功", null);
    }

    @Operation(summary = "灵感版本列表")
    @GetMapping("/{id}/versions")
    public Result<java.util.List<java.util.Map<String, Object>>> listVersions(@PathVariable Long id) {
        return Result.success(inspireService.listVersions(id));
    }

    @Operation(summary = "灵感版本详情")
    @GetMapping("/{id}/versions/{versionId}")
    public Result<java.util.Map<String, Object>> getVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.success(inspireService.getVersion(versionId));
    }

    @PostMapping("/collect/folder")
    @Operation(summary = "创建收藏文件夹")
    public Result<CollectFolder> createFolder(@RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        String name = body.get("name");
        String icon = body.get("icon");
        return Result.success(inspireService.createCollectFolder(userId, name, icon));
    }

    @GetMapping("/collect/folders")
    @Operation(summary = "收藏文件夹列表")
    public Result<List<CollectFolder>> folders(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(inspireService.getCollectFolders(userId));
    }

    @DeleteMapping("/collect/folder/{id}")
    @Operation(summary = "删除收藏文件夹")
    public Result<Void> deleteFolder(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        inspireService.deleteCollectFolder(userId, id);
        return Result.success();
    }

    @PutMapping("/collect/folder/{id}")
    @Operation(summary = "重命名收藏文件夹")
    public Result<Void> renameFolder(@PathVariable("id") Long id, @RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        String name = body.get("name");
        inspireService.renameCollectFolder(userId, id, name);
        return Result.success();
    }

    @PostMapping("/collect/{id}/folder")
    @Operation(summary = "收藏到指定文件夹")
    public Result<Void> collectToFolder(@PathVariable("id") Long inspireId,
                                         @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Long folderId = body.get("folderId") != null ? Long.valueOf(body.get("folderId").toString()) : null;
        inspireService.collectToFolder(userId, inspireId, folderId);
        return Result.success();
    }

    @GetMapping("/collect/list")
    @Operation(summary = "按文件夹获取收藏列表")
    public Result<List<InspireVO>> collectList(@RequestParam(required = false) Long folderId,
                                                HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(inspireService.listCollectsByFolder(userId, folderId));
    }



    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-Inspire-UserId");
        return userId != null ? Long.parseLong(userId) : null;
    }


    @PutMapping("/collect/{id}/move")
    @Operation(summary = "移动收藏到文件夹")
    public Result<Void> moveCollect(@PathVariable("id") Long inspireId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Long folderId = body.get("folderId") != null ? Long.valueOf(body.get("folderId").toString()) : null;
        inspireService.moveCollectToFolder(userId, inspireId, folderId);
        return Result.success();
    }


    @GetMapping("/public/hot-tags")
    @Operation(summary = "热门标签")
    public Result<java.util.List<java.util.Map<String, Object>>> hotTags() {
        return Result.success(inspireService.getHotTags());
    }


    @PostMapping("/public/suggest-images")
    @Operation(summary = "AI 配图建议", description = "配置了 Unsplash Access Key 则搜索真实图片，否则回退到 picsum 随机占位图")
    public Result<java.util.List<String>> suggestImages(@RequestBody Map<String, String> body) {
        String keyword = body.getOrDefault("keyword", "");
        java.util.List<String> urls = new java.util.ArrayList<>();
        
        // 尝试 Unsplash API
        if (unsplashAccessKey != null && !unsplashAccessKey.isBlank()) {
            try {
                // 截短并清理关键词（去除全角标点等 Unsplash 不支持的字符）
                String shortKw = keyword.length() > 50 ? keyword.substring(0, 50) : keyword;
                String clean = shortKw.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
                if (clean.isEmpty()) clean = shortKw.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "").trim();
                if (clean.isEmpty()) clean = "inspiration";
                String encoded = java.net.URLEncoder.encode(clean, "UTF-8").replace("+", "%20");
                log.info("Unsplash URL: https://api.unsplash.com/search/photos?query={}&per_page=6", encoded);
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.unsplash.com/search/photos?query=" + encoded + "&per_page=6"))
                    .header("Authorization", "Client-ID " + unsplashAccessKey)
                    .header("User-Agent", "Mozilla/5.0 (compatible; InspireAI/1.0)")
                    .build();
                java.net.http.HttpResponse<String> resp = HTTP_CLIENT.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                log.info("Unsplash response status: {}", resp.statusCode());
                if (resp.statusCode() == 200) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(resp.body());
                    com.fasterxml.jackson.databind.JsonNode results = root.get("results");
                    if (results != null && results.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode r : results) {
                            com.fasterxml.jackson.databind.JsonNode u = r.get("urls");
                            if (u != null && u.get("small") != null) {
                                urls.add(u.get("small").asText());
                            }
                        }
                    }
                } else {
                    String rspBody = resp.body();
                    log.warn("Unsplash API 返回非200: status={}", resp.statusCode());
                    if (rspBody != null && !rspBody.isEmpty()) {
                        log.warn("Unsplash body: {}", rspBody.length() > 500 ? rspBody.substring(0, 500) + "..." : rspBody);
                    }
                }
            } catch (Exception e) {
                log.warn("Unsplash API 请求失败，回退到 picsum: {}", e.getMessage());
            }
        }
        
        // 回退：picsum 随机占位图（无 key 或 API 失败时使用）
        if (urls.isEmpty() && unsplashAccessKey != null && !unsplashAccessKey.isBlank()) {
            log.info("AI配图回退到picsum: keyword={}", keyword);
        } else if (urls.isEmpty() && (unsplashAccessKey == null || unsplashAccessKey.isBlank())) {
            log.info("AI配图使用picsum(未配置Unsplash): keyword={}", keyword);
        }
        if (urls.isEmpty()) {
            int seed = keyword.hashCode() & 0x7fffffff;
            for (int i = 0; i < 6; i++) {
                urls.add("https://picsum.photos/seed/" + (seed + i) + "/400/300");
            }
        }
        return Result.success(urls);
    }

}
