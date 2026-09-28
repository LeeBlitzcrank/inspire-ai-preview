package com.inspire.platform.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "inspire.rag")
public class RagProperties {

    private boolean enabled = true;
    private String indexName = "inspire_rag_index";
    private int embeddingDims = 768;
    private int embeddingBatchSize = 32;
    private int defaultTopK = 8;
    private int candidateK = 30;
    private int syncBatchSize = 50;
    private long syncDelayMs = 30_000;
    private String elasticsearchHost = "localhost:9200";
    private String coreBaseUrl = "http://localhost:8083";
    private String imageBaseUrl = "https://img.20sherry.com";
    private String adminToken = "";

    private boolean imageAnalysisEnabled = true;
    private boolean ollamaEmbeddingEnabled = false;
    private boolean ollamaCaptionEnabled = false;
    private String ollamaUrl = "http://localhost:11434";
    private String embeddingModel = "embeddinggemma:300m";
    private String captionModel = "qwen2.5vl:7b";

    private String deepseekApiKey = "";
    private String deepseekApiUrl = "https://api.deepseek.com/v1/chat/completions";
    private String deepseekModel = "deepseek-chat";
}
