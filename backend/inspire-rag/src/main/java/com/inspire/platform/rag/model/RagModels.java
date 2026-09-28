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
