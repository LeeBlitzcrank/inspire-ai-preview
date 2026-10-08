package com.inspire.platform.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "inspire.support-rag")
public class SupportProperties {

    private boolean enabled = true;
    private String indexName = "support_knowledge_index";
    private String rootDir = ".";
    private String knowledgeFile = "docs/current/项目客服知识库.md";
    private int chunkSize = 1200;
    private int chunkOverlap = 150;
    private int defaultTopK = 6;
    private int candidateK = 30;
    private long scanDelayMs = 600_000;
    private long maxFileBytes = 2_000_000;

    private String answerProvider = "fallback";
    private String ollamaUrl = "http://localhost:11434";
    private String ollamaModel = "deepseek-r1:8b";
    private int ollamaTimeoutSeconds = 120;
}
