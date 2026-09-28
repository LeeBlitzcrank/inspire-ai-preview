package com.inspire.platform.rag.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.RagResponse;
import com.inspire.platform.rag.service.ElasticsearchRagStore;
import com.inspire.platform.rag.service.EmbeddingService;
import com.inspire.platform.rag.service.RagAnswerService;
import com.inspire.platform.rag.service.RagIndexService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagAnswerService answerService;
    private final RagIndexService indexService;
    private final ElasticsearchRagStore store;
    private final EmbeddingService embeddingService;
    private final RagProperties properties;

    public RagController(RagAnswerService answerService,
                         RagIndexService indexService,
                         ElasticsearchRagStore store,
                         EmbeddingService embeddingService,
                         RagProperties properties) {
        this.answerService = answerService;
        this.indexService = indexService;
        this.store = store;
        this.embeddingService = embeddingService;
        this.properties = properties;
    }

    @Operation(summary = "多模态 RAG 问答")
    @PostMapping("/ask")
    public Result<RagResponse> ask(@Valid @RequestBody RagRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(answerService.ask(
                request.query(),
                request.imageBase64(),
                request.topK(),
                userId
        ));
    }

    @Operation(summary = "多模态 RAG 检索，不调用生成模型")
    @PostMapping("/search")
    public Result<RagResponse> search(@Valid @RequestBody RagRequest request,
                                      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(answerService.search(
                request.query(),
                request.imageBase64(),
                request.topK(),
                userId
        ));
    }

    @Operation(summary = "RAG 索引状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        var embedding = embeddingService.embed("RAG 状态检查");
        return Result.success(Map.of(
                "enabled", properties.isEnabled(),
                "indexName", properties.getIndexName(),
                "published", indexService.publishedCount(),
                "indexed", indexService.indexedCount(),
                "elasticsearchDocuments", store.count(),
                "embeddingProvider", embedding.provider(),
                "visionEnabled", properties.isOllamaCaptionEnabled(),
                "answerEnabled", !properties.getDeepseekApiKey().isBlank()
        ));
    }

    @Operation(summary = "内部重建 RAG 索引")
    @PostMapping("/admin/reindex")
    public Result<Map<String, Object>> reindex(
            @RequestParam(defaultValue = "500") int limit,
            @RequestHeader(value = "X-Rag-Token", required = false) String token) {
        if (properties.getAdminToken().isBlank() || !properties.getAdminToken().equals(token)) {
            return Result.forbidden();
        }
        int indexed = indexService.indexAll(limit);
        return Result.success(Map.of("indexed", indexed, "indexedTotal", indexService.indexedCount()));
    }

    @Operation(summary = "内部查询 RAG 重建进度")
    @GetMapping("/admin/reindex/status")
    public Result<Map<String, Object>> reindexStatus(
            @RequestHeader(value = "X-Rag-Token", required = false) String token) {
        if (properties.getAdminToken().isBlank() || !properties.getAdminToken().equals(token)) {
            return Result.forbidden();
        }
        RagIndexService.ReindexState state = indexService.reindexState();
        long now = System.currentTimeMillis();
        long elapsedMs = state.startedAt() <= 0
                ? 0
                : Math.max(0, (state.finishedAt() > 0 ? state.finishedAt() : now) - state.startedAt());
        int progressPercent = state.totalChunks() <= 0
                ? 0
                : Math.min(100, (int) Math.round(state.processedChunks() * 100d / state.totalChunks()));
        long estimatedRemainingMs = -1;
        if (state.running() && state.processedChunks() > 0 && state.totalChunks() > state.processedChunks()) {
            estimatedRemainingMs = elapsedMs * (state.totalChunks() - state.processedChunks())
                    / state.processedChunks();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", state.running());
        result.put("phase", state.phase());
        result.put("totalDocuments", state.totalDocuments());
        result.put("processedDocuments", state.processedDocuments());
        result.put("totalChunks", state.totalChunks());
        result.put("processedChunks", state.processedChunks());
        result.put("progressPercent", progressPercent);
        result.put("elapsedMs", elapsedMs);
        result.put("estimatedRemainingMs", estimatedRemainingMs);
        result.put("startedAt", state.startedAt());
        result.put("finishedAt", state.finishedAt());
        result.put("errorMessage", state.errorMessage());
        return Result.success(result);
    }

    public record RagRequest(
            @Size(max = 500, message = "问题不能超过500字") String query,
            @Size(max = 8_000_000, message = "图片数据过大") String imageBase64,
            Integer topK
    ) {
    }
}
