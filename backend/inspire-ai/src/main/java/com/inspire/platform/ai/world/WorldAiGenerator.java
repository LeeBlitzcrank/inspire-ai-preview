/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldAiGenerator.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.world;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.ai.world.WorldModels.CharacterView;
import com.inspire.platform.ai.world.WorldModels.GenerateSeedRequest;
import com.inspire.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class WorldAiGenerator {

    private static final int MIN_CONTENT_LENGTH = 800;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int LENGTH_RETRY_LIMIT = 3;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public WorldAiGenerator(ObjectMapper objectMapper,
                            RestTemplate restTemplate,
                            @Value("${inspire.ai.deepseek.api-key:}") String apiKey,
                            @Value("${inspire.ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}") String apiUrl,
                            @Value("${inspire.ai.deepseek.model:deepseek-chat}") String model) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public SeedDraft generateSeed(GenerateSeedRequest request) {
        String prompt = """
                你是一个平行世界叙事设计师。请基于下面的原文锚点，生成一个可长期扩展的世界种子。

                原文名称：%s
                原文作者：%s
                原文内容：
                %s
                创作约束：%s

                只返回JSON，不要Markdown：
                {
                  "title":"世界名称",
                  "background":"世界初始背景，必须是850到950个中文字符，分为8到9个自然段，每段约100字，交代人物、关系、环境、历史、冲突和未解问题",
                  "rules":["世界规则1","世界规则2","世界规则3"],
                  "characters":[{"name":"角色名","description":"角色描述"}],
                  "question":"当前最重要的未解问题",
                  "lines":[
                    {"title":"世界线名称","variable":"改变的关键条件","environment":"当前环境","characterState":"角色状态","question":"这条世界线的未解问题"}
                  ]
                }
                要求 lines 生成3到5条差异明显的世界线。不得改写或伪造原文，不得声称AI内容是原文。
                """.formatted(
                request.sourceTitle(),
                Objects.toString(request.sourceAuthor(), ""),
                truncate(request.sourceText(), 6000),
                Objects.toString(request.guidance(), "")
        );
        JsonNode data = callForJson(prompt, "world-seed");
        String background = enforceLength(data.path("background").asText(""));
        List<String> rules = readStringList(data.path("rules"));
        List<CharacterView> characters = new ArrayList<>();
        for (JsonNode node : data.path("characters")) {
            characters.add(new CharacterView(
                    node.path("name").asText(""),
                    node.path("description").asText("")
            ));
        }
        List<LineDraft> lines = new ArrayList<>();
        for (JsonNode line : data.path("lines")) {
            lines.add(new LineDraft(
                    line.path("title").asText("平行世界线"),
                    line.path("variable").asText(""),
                    line.path("environment").asText(""),
                    line.path("characterState").asText(line.path("character_state").asText("")),
                    line.path("question").asText("")
            ));
        }
        return new SeedDraft(
                data.path("title").asText(request.sourceTitle() + "平行世界"),
                background,
                rules,
                characters,
                data.path("question").asText(""),
                lines
        );
    }

    public ChapterDraft generateChapter(ChapterContext context, String choiceKey, String choiceText) {
        String prompt = """
                你正在续写一条平行世界线。必须保持人物、规则、地点和前文连续，不得把分支内容写成原文。

                原文：%s
                原线背景：%s
                当前世界线：%s
                关键变量：%s
                当前环境：%s
                角色状态：%s
                上一章结尾：%s
                用户选择：%s

                只返回JSON：
                {
                  "title":"下一章标题",
                  "content":"下一章完整正文，必须是850到950个中文字符，分为9到10个自然段，每段约90到100字，包含场景、人物行动、冲突升级、结果和新的悬念",
                  "nextQuestion":"下一章结束时最重要的新问题"
                }
                """.formatted(
                truncate(context.sourceText(), 3000),
                truncate(context.background(), 5000),
                context.lineTitle(),
                context.variable(),
                context.environment(),
                context.characterState(),
                truncate(context.latestContent(), 4000),
                Objects.toString(choiceText, choiceKey)
        );
        JsonNode data = callForJson(prompt, "world-chapter");
        String content = enforceLength(data.path("content").asText(""));
        return new ChapterDraft(
                data.path("title").asText("下一章"),
                content,
                data.path("nextQuestion").asText("")
        );
    }

    private JsonNode callForJson(String prompt, String cacheTag) {
        String content = callModel(prompt, 0.75, true);
        try {
            return objectMapper.readTree(cleanJson(content));
        } catch (Exception e) {
            log.warn("世界种子JSON解析失败 tag={}: {}", cacheTag, e.getMessage());
            throw new BusinessException(502, "模型返回格式不正确，请重试");
        }
    }

    private String callForText(String prompt) {
        return cleanText(callModel(prompt, 0.35, false));
    }

    private String callModel(String prompt, double temperature, boolean jsonMode) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(503, "DeepSeek 未配置，无法生成世界内容");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", 4096);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        jsonMode
                                ? "你是平行世界叙事编辑。严格返回JSON，不使用Markdown，不伪造原文，不把AI推演称为真实历史。"
                                : "你是中文长篇编辑。只返回完整中文正文，不使用Markdown，不解释改写过程，不伪造原文。"),
                Map.of("role", "user", "content", prompt)
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonNode response = restTemplate.postForObject(
                apiUrl,
                new HttpEntity<>(body, headers),
                JsonNode.class
        );
        String content = response == null
                ? ""
                : response.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(502, "模型未返回世界内容");
        }
        return content;
    }

    private String enforceLength(String content) {
        String current = cleanText(content);
        for (int i = 0; i < LENGTH_RETRY_LIMIT && !isLengthValid(current); i++) {
            int currentLength = contentLength(current);
            log.warn("世界内容长度不合格 actual={}, retry={}/{}",
                    currentLength, i + 1, LENGTH_RETRY_LIMIT);
            String fixPrompt = """
                    请重写下面这段平行世界正文，只返回完整正文，不要JSON，不要标题以外的说明。

                    当前非空白字符数：%d
                    目标长度：850到950个中文字符，绝对不能少于800，绝对不能超过1000。
                    偏短时补足场景、动作、对话和心理变化；偏长时删掉重复描写和解释。
                    保持人物、地点、因果和结尾悬念连续，不要删掉最后的转折。

                    正文：
                    %s
                    """.formatted(currentLength, truncate(current, 7000));
            current = callForText(fixPrompt);
        }
        if (contentLength(current) > MAX_CONTENT_LENGTH) {
            current = trimToMaxLength(current);
        }
        if (!isLengthValid(current)) {
            throw new BusinessException(502, "模型生成内容长度不合格，请重试");
        }
        return current;
    }

    private String trimToMaxLength(String content) {
        String text = cleanText(content);
        int visible = 0;
        int cutIndex = text.length();
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) visible++;
            if (visible >= MAX_CONTENT_LENGTH) {
                cutIndex = i + 1;
                break;
            }
        }
        int sentenceEnd = -1;
        for (int i = cutIndex - 1; i >= Math.max(0, cutIndex - 180); i--) {
            char ch = text.charAt(i);
            if (ch == '。' || ch == '！' || ch == '？' || ch == '\n') {
                sentenceEnd = i + 1;
                break;
            }
        }
        return text.substring(0, sentenceEnd > 0 ? sentenceEnd : cutIndex).trim();
    }

    private boolean isLengthValid(String content) {
        int length = contentLength(content);
        return length >= MIN_CONTENT_LENGTH && length <= MAX_CONTENT_LENGTH;
    }

    private String cleanText(String value) {
        return Objects.toString(value, "")
                .replaceAll("```[a-zA-Z]*", "")
                .replace("```", "")
                .replace("\r\n", "\n")
                .trim();
    }

    private int contentLength(String value) {
        return cleanText(value).replaceAll("\\s", "").length();
    }

    private String cleanJson(String value) {
        return Objects.toString(value, "")
                .replaceAll("```json\\s*|```\\s*", "")
                .trim();
    }

    private String truncate(String value, int max) {
        String text = Objects.toString(value, "");
        return text.length() <= max ? text : text.substring(0, max);
    }

    private List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (StringUtils.hasText(item.asText(""))) list.add(item.asText(""));
            }
        }
        return list;
    }

    public record SeedDraft(
            String title,
            String background,
            List<String> rules,
            List<CharacterView> characters,
            String question,
            List<LineDraft> lines
    ) {
    }

    public record LineDraft(
            String title,
            String variable,
            String environment,
            String characterState,
            String question
    ) {
    }

    public record ChapterDraft(String title, String content, String nextQuestion) {
    }

    public record ChapterContext(
            String sourceText,
            String background,
            String lineTitle,
            String variable,
            String environment,
            String characterState,
            String latestContent
    ) {
    }
}
