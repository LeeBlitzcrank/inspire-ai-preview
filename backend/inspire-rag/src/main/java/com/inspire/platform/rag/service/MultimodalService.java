/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/service/MultimodalService.java
 * 所属模块：多模态 RAG 模块，负责索引、向量检索、问答和同步
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.inspire.platform.rag.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MultimodalService {

    private final RagProperties properties;
    private final RestTemplate restTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public MultimodalService(RagProperties properties, RestTemplate ragRestTemplate) {
        this.properties = properties;
        this.restTemplate = ragRestTemplate;
    }

    public String describeImageBase64(String imageBase64) {
        byte[] bytes = decodeBase64(imageBase64);
        if (bytes.length == 0) return "";
        if (properties.isOllamaCaptionEnabled()) {
            try {
                return callOllamaCaption(bytes);
            } catch (Exception e) {
                log.warn("Ollama 图片描述失败，降级本地视觉特征: {}", e.getMessage());
            }
        }
        return describeLocal(bytes, "");
    }

    public String describeImageUrl(String imageUrl, String fallback) {
        if (!properties.isImageAnalysisEnabled() || imageUrl == null || imageUrl.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        try {
            byte[] bytes = downloadImage(imageUrl);
            if (properties.isOllamaCaptionEnabled()) {
                try {
                    String caption = callOllamaCaption(bytes);
                    if (!caption.isBlank()) return caption;
                } catch (Exception e) {
                    log.debug("Ollama 图片描述失败: {}", e.getMessage());
                }
            }
            String local = describeLocal(bytes, "");
            return local.isBlank() ? fallback : (fallback + "；视觉特征：" + local);
        } catch (Exception e) {
            log.debug("图片特征提取失败 url={}: {}", imageUrl, e.getMessage());
            return fallback == null ? "" : fallback;
        }
    }

    private String callOllamaCaption(byte[] imageBytes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getCaptionModel());
        body.put("stream", false);
        body.put("prompt", "请用中文简洁描述这张图片的主体、场景、色彩、光线和氛围，最多80字，不要Markdown。");
        body.put("images", List.of(Base64.getEncoder().encodeToString(imageBytes)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonNode response = restTemplate.postForObject(
                properties.getOllamaUrl().replaceAll("/+$", "") + "/api/generate",
                new HttpEntity<>(body, headers),
                JsonNode.class
        );
        return response == null ? "" : response.path("response").asText("").trim();
    }

    private byte[] downloadImage(String imageUrl) throws Exception {
        String url = imageUrl;
        if (url.startsWith("https://img.20sherry.com/")) {
            url = url.replace("https://img.20sherry.com/", "http://nginx-image/");
        } else if (url.startsWith("/uploads/")) {
            url = properties.getCoreBaseUrl().replaceAll("/+$", "") + url;
        } else if (url.startsWith("/upload/")) {
            url = "http://nginx-image" + url;
        }
        // WebP 变体常无法被标准 ImageIO 解码，索引时优先尝试对应原图。
        String original = url.replaceAll("_w(200|400|800)\\.webp(?=\\?|$)", ".jpg");
        byte[] bytes = getBytes(original);
        if (bytes.length == 0 && !original.equals(url)) {
            bytes = getBytes(url);
        }
        return bytes;
    }

    private byte[] getBytes(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "image/*")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? response.body()
                    : new byte[0];
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private byte[] decodeBase64(String value) {
        if (value == null || value.isBlank()) return new byte[0];
        String data = value.contains(",") ? value.substring(value.indexOf(',') + 1) : value;
        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    private String describeLocal(byte[] bytes, String fallback) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) return fallback;
            int width = source.getWidth();
            int height = source.getHeight();
            double red = 0, green = 0, blue = 0, saturation = 0, luminance = 0;
            int count = 0;
            for (int y = 0; y < height; y += Math.max(1, height / 12)) {
                for (int x = 0; x < width; x += Math.max(1, width / 12)) {
                    int rgb = source.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    red += r;
                    green += g;
                    blue += b;
                    int max = Math.max(r, Math.max(g, b));
                    int min = Math.min(r, Math.min(g, b));
                    saturation += max == 0 ? 0 : (double) (max - min) / max;
                    luminance += 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    count++;
                }
            }
            if (count == 0) return fallback;
            red /= count;
            green /= count;
            blue /= count;
            saturation /= count;
            luminance /= count;
            List<String> labels = new ArrayList<>();
            if (red > green && red > blue) labels.add("暖色");
            else if (blue >= red && blue >= green) labels.add("冷色");
            else labels.add("自然中性色");
            if (saturation < 0.25) labels.add("低饱和");
            else if (saturation > 0.55) labels.add("高饱和");
            if (luminance < 82) labels.add("暗调");
            else if (luminance > 185) labels.add("明亮");
            else labels.add("中间调");
            double ratio = (double) width / Math.max(1, height);
            if (ratio > 1.25) labels.add("横向构图");
            else if (ratio < 0.8) labels.add("竖向构图");
            else labels.add("方形构图");
            return String.join("、", labels);
        } catch (Exception e) {
            return fallback;
        }
    }
}
