/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/model/RagModels.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.model;

import java.util.List;

public final class RagModels {

    private RagModels() {
    }

    public record ChunkDocument(
            long inspireId,
            String chunkId,
            String modality,
            String title,
            String content,
            String caption,
            String tag,
            String city,
            long authorId,
            Long seriesId,
            String imageUrl,
            float[] embedding
    ) {
    }

    public record SearchHit(
            long inspireId,
            String chunkId,
            String modality,
            String title,
            String content,
            String caption,
            String tag,
            String imageUrl,
            double score
    ) {
    }

    public record RagSource(
            long id,
            String title,
            String excerpt,
            String tag,
            String image,
            double score,
            String url
    ) {
    }

    public record RagResponse(
            String answer,
            List<RagSource> sources,
            String mode,
            String queryCaption,
            long tookMs
    ) {
    }

    public record EmbeddingResult(float[] vector, String provider) {
    }
}
