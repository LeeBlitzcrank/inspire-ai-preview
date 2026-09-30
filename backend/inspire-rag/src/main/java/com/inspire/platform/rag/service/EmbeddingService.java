/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/service/EmbeddingService.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.inspire.platform.rag.config.RagProperties;
import com.inspire.platform.rag.model.RagModels.EmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

@Slf4j
@Service
public class EmbeddingService {

    private final RagProperties properties;
    private final RestTemplate restTemplate;
    private volatile Boolean ollamaAvailable;

    public EmbeddingService(RagProperties properties, RestTemplate ragRestTemplate) {
        this.properties = properties;
        this.restTemplate = ragRestTemplate;
    }

    public EmbeddingResult embed(String text) {
        String normalized = normalize(text);
        if (properties.isOllamaEmbeddingEnabled() && !normalized.isBlank()) {
            try {
                float[] vector = embedWithOllama(normalized);
                if (vector.length == properties.getEmbeddingDims()) {
                    ollamaAvailable = true;
                    return new EmbeddingResult(vector, "ollama:" + properties.getEmbeddingModel());
                }
                log.warn("Ollama embedding 维度不匹配: expected={}, actual={}",
                        properties.getEmbeddingDims(), vector.length);
                ollamaAvailable = false;
            } catch (Exception e) {
                ollamaAvailable = false;
                log.warn("Ollama embedding 不可用，降级本地哈希向量: {}", e.getMessage());
            }
        }
        return new EmbeddingResult(hashEmbedding(normalized), "local-hash");
    }

    public List<EmbeddingResult> embedBatch(List<String> texts) {
        return embedBatch(texts, null);
    }

    public List<EmbeddingResult> embedBatch(List<String> texts, IntConsumer progressCallback) {
        if (texts == null || texts.isEmpty()) return List.of();
        if (!properties.isOllamaEmbeddingEnabled()) {
            List<EmbeddingResult> results = texts.stream()
                    .map(this::normalize)
                    .map(text -> new EmbeddingResult(hashEmbedding(text), "local-hash"))
                    .toList();
            if (progressCallback != null) progressCallback.accept(results.size());
            return results;
        }

        List<EmbeddingResult> results = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, Math.min(properties.getEmbeddingBatchSize(), 128));
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(texts.size(), start + batchSize);
            List<String> batch = texts.subList(start, end).stream().map(this::normalize).toList();
            try {
                Map<String, Object> body = Map.of(
                        "model", properties.getEmbeddingModel(),
                        "input", batch
                );
                JsonNode response = restTemplate.postForObject(
                        properties.getOllamaUrl().replaceAll("/+$", "") + "/api/embed",
                        jsonEntity(body),
                        JsonNode.class
                );
                JsonNode vectors = response == null ? null : response.path("embeddings");
                if (vectors == null || !vectors.isArray() || vectors.size() != batch.size()) {
                    throw new IllegalStateException("invalid batch embedding response");
                }
                for (JsonNode node : vectors) {
                    float[] vector = vectorFromNode(node);
                    if (vector.length != properties.getEmbeddingDims()) {
                        throw new IllegalStateException("embedding dimension mismatch");
                    }
                    results.add(new EmbeddingResult(vector, "ollama:" + properties.getEmbeddingModel()));
                }
                ollamaAvailable = true;
                log.info("Ollama embedding 批次完成: {}/{}", end, texts.size());
                if (progressCallback != null) progressCallback.accept(end);
            } catch (Exception e) {
                ollamaAvailable = false;
                throw new IllegalStateException("Ollama 批量 embedding 失败: " + e.getMessage(), e);
            }
        }
        return results;
    }

    public String provider() {
        if (!properties.isOllamaEmbeddingEnabled()) {
            return "local-hash";
        }
        return Boolean.TRUE.equals(ollamaAvailable)
                ? "ollama:" + properties.getEmbeddingModel()
                : "local-hash(fallback)";
    }

    private float[] embedWithOllama(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getEmbeddingModel());
        body.put("prompt", text);
        JsonNode response = restTemplate.postForObject(
                properties.getOllamaUrl().replaceAll("/+$", "") + "/api/embeddings",
                jsonEntity(body),
                JsonNode.class
        );
        if (response == null || response.path("embedding").isMissingNode()) {
            throw new IllegalStateException("empty embedding response");
        }
        float[] vector = vectorFromNode(response.path("embedding"));
        normalize(vector);
        return vector;
    }

    private float[] vectorFromNode(JsonNode node) {
        float[] vector = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = (float) node.get(i).asDouble();
        }
        return vector;
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 零依赖降级向量：中文字符、双字词、英文词和空白无关字符共同参与哈希。
     * 它不是神经网络向量，但能让 RAG 在没有 Ollama 时保持可运行。
     */
    private float[] hashEmbedding(String text) {
        int dims = Math.max(128, properties.getEmbeddingDims());
        float[] vector = new float[dims];
        if (text.isBlank()) {
            vector[0] = 1f;
            return vector;
        }
        List<String> tokens = tokens(text);
        for (String token : tokens) {
            int hash = murmurHash(token.getBytes(StandardCharsets.UTF_8));
            int idx = Math.floorMod(hash, dims);
            float sign = ((hash >>> 31) & 1) == 0 ? 1f : -1f;
            vector[idx] += sign;
        }
        normalize(vector);
        return vector;
    }

    private List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        List<Integer> cjk = new ArrayList<>();
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            offset += Character.charCount(cp);
            if (isCjk(cp)) {
                if (!latin.isEmpty()) {
                    tokens.add(latin.toString());
                    latin.setLength(0);
                }
                cjk.add(cp);
            } else {
                if (Character.isLetterOrDigit(cp)) {
                    latin.appendCodePoint(cp);
                } else if (!latin.isEmpty()) {
                    tokens.add(latin.toString());
                    latin.setLength(0);
                }
            }
        }
        if (!latin.isEmpty()) tokens.add(latin.toString());
        for (int i = 0; i < cjk.size(); i++) {
            tokens.add(new String(Character.toChars(cjk.get(i))));
            if (i + 1 < cjk.size()) {
                tokens.add(new String(Character.toChars(cjk.get(i)))
                        + new String(Character.toChars(cjk.get(i + 1))));
            }
        }
        for (int i = 0; i + 2 < cjk.size(); i += 2) {
            tokens.add(new String(Character.toChars(cjk.get(i)))
                    + new String(Character.toChars(cjk.get(i + 1)))
                    + new String(Character.toChars(cjk.get(i + 2))));
        }
        return tokens;
    }

    private boolean isCjk(int cp) {
        return (cp >= 0x4e00 && cp <= 0x9fff)
                || (cp >= 0x3400 && cp <= 0x4dbf)
                || (cp >= 0xf900 && cp <= 0xfaff);
    }

    private int murmurHash(byte[] data) {
        int h = 0x811c9dc5;
        for (byte b : data) {
            h ^= b & 0xff;
            h *= 0x01000193;
        }
        return h;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) sum += value * value;
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            if (vector.length > 0) vector[0] = 1f;
            return;
        }
        for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm);
    }
}
