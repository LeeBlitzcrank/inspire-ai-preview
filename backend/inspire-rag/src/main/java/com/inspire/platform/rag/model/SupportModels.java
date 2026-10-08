package com.inspire.platform.rag.model;

import java.util.List;

public final class SupportModels {

    private SupportModels() {
    }

    public record SupportChunkDocument(
            String chunkId,
            String docId,
            String title,
            String section,
            String content,
            String filePath,
            String updatedAt,
            float[] embedding
    ) {
    }

    public record SupportKnowledgeHit(
            String chunkId,
            String docId,
            String title,
            String section,
            String content,
            String filePath,
            String updatedAt,
            double score
    ) {
    }

    public record SupportSource(
            String title,
            String section,
            String excerpt,
            String filePath,
            double score
    ) {
    }

    public record SupportResponse(
            String answer,
            List<SupportSource> sources,
            String provider,
            long tookMs
    ) {
    }

    public record SupportIndexState(
            boolean running,
            String status,
            int documentCount,
            int chunkCount,
            String fingerprint,
            String errorMessage,
            String indexedAt
    ) {
    }

    public record SupportTicketCreated(
            String ticketNo,
            String accessToken,
            String status,
            String createdAt
    ) {
    }

    public record SupportTicketMessageView(
            String senderType,
            String content,
            String createTime
    ) {
    }

    public record SupportTicketAttachment(
            String url,
            String thumbUrl,
            String fileType,
            String originalName,
            String duration
    ) {
    }

    public record SupportTicketView(
            String ticketNo,
            String status,
            String issue,
            String contactValue,
            String lastReply,
            String assigneeName,
            String createTime,
            String updateTime,
            List<SupportTicketMessageView> messages,
            List<SupportTicketAttachment> attachments
    ) {
    }
}
