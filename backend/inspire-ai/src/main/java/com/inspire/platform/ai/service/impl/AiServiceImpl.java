package com.inspire.platform.ai.service.impl;

import com.inspire.platform.ai.dto.*;
import com.inspire.platform.ai.service.AiService;
import com.inspire.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /**
     * 关键词 → 灵感候选 映射表
     * 模拟 Codex AI 的返回结果
     */
    private static final Map<String, List<InspirationCandidate>> KNOWLEDGE_BASE = new LinkedHashMap<>();

    static {
        KNOWLEDGE_BASE.put("鸡腿", Arrays.asList(
                new InspirationCandidate(1, "鸡腿的五种神仙吃法", "外酥里嫩，厨房小白也能轻松搞定！一周不重样。", "美食"),
                new InspirationCandidate(2, "减脂期也能吃的鸡腿做法", "低卡高蛋白，不用油炸照样香喷喷。", "美食")
        ));
        KNOWLEDGE_BASE.put("运动", Arrays.asList(
                new InspirationCandidate(1, "15分钟居家晨间燃脂计划", "无器械不伤膝，坚持两周见效。", "运动"),
                new InspirationCandidate(2, "新手跑步入门指南", "从零到5公里，科学训练不受伤。", "运动")
        ));
        KNOWLEDGE_BASE.put("跑步", Arrays.asList(
                new InspirationCandidate(1, "科学跑步减脂计划", "不伤膝盖慢跑节奏，完整热身拉伸流程。", "运动"),
                new InspirationCandidate(2, "夜跑路线推荐与注意事项", "安全又出片的城市夜跑攻略。", "运动")
        ));
        KNOWLEDGE_BASE.put("健身", Arrays.asList(
                new InspirationCandidate(1, "居家无器械健身教程", "全套徒手塑形动作，早晚15分钟即可。", "运动"),
                new InspirationCandidate(2, "健身房新手器械指南", "从哑铃到龙门架，一篇看懂所有器械用法。", "运动")
        ));
        KNOWLEDGE_BASE.put("穿搭", Arrays.asList(
                new InspirationCandidate(1, "清爽夏日穿搭灵感", "低饱和简约搭配，学生通勤通用。", "穿搭"),
                new InspirationCandidate(2, "一衣多穿：白T恤的10种搭配", "基础款穿出高级感，衣柜利用率翻倍。", "穿搭")
        ));
        KNOWLEDGE_BASE.put("电影", Arrays.asList(
                new InspirationCandidate(1, "高分悬疑电影推荐", "反转细腻、逻辑完整，烧脑爱好者必看。", "电影"),
                new InspirationCandidate(2, "治愈系纪录片合集", "拓宽眼界、舒缓情绪，看完内心变柔软。", "电影")
        ));
        KNOWLEDGE_BASE.put("文案", Arrays.asList(
                new InspirationCandidate(1, "短视频万能文案模板", "美食、生活、好物通用高完播率文案。", "文案"),
                new InspirationCandidate(2, "小众高级朋友圈短句", "温柔简约，告别土味文案。", "文案")
        ));
        KNOWLEDGE_BASE.put("美食", Arrays.asList(
                new InspirationCandidate(1, "家庭火锅底料搭配技巧", "番茄、牛油、清汤三种锅底调配方案。", "美食"),
                new InspirationCandidate(2, "上班族10分钟快手早餐", "简单营养，每天多睡20分钟。", "美食")
        ));
        KNOWLEDGE_BASE.put("旅游", Arrays.asList(
                new InspirationCandidate(1, "周末短途旅行攻略", "两天一夜小众景点推荐，说走就走。", "旅游"),
                new InspirationCandidate(2, "一个人旅行的安全指南", "独自出行必备清单+防坑建议。", "旅游")
        ));
        KNOWLEDGE_BASE.put("摄影", Arrays.asList(
                new InspirationCandidate(1, "手机摄影构图技巧", "三分法、引导线，随手拍出大片感。", "摄影"),
                new InspirationCandidate(2, "城市夜景拍摄参数大全", "三脚架+长曝光，拍出赛博朋克风。", "摄影")
        ));
    }

    @Override
    public AiGenerateResponse generate(AiGenerateRequest request) {
        String keyword = request.getKeyword().trim();

        // 精确匹配 → 返回对应灵感
        if (KNOWLEDGE_BASE.containsKey(keyword)) {
            log.info("AI生成灵感: keyword={}, source=exact_match", keyword);
            return new AiGenerateResponse(keyword, KNOWLEDGE_BASE.get(keyword));
        }

        // 模糊匹配 → 找包含关键词的条目
        List<InspirationCandidate> fuzzyResults = KNOWLEDGE_BASE.entrySet().stream()
                .filter(e -> e.getKey().contains(keyword) || keyword.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .limit(2)
                .collect(Collectors.toList());

        if (!fuzzyResults.isEmpty()) {
            log.info("AI生成灵感: keyword={}, source=fuzzy_match", keyword);
            return new AiGenerateResponse(keyword, fuzzyResults);
        }

        // 无匹配 → AI动态生成
        List<InspirationCandidate> generated = generateFallback(keyword);
        log.info("AI生成灵感: keyword={}, source=generated", keyword);
        return new AiGenerateResponse(keyword, generated);
    }

    @Override
    public void select(AiSelectRequest request, Long userId) {
        log.info("用户选中灵感: userId={}, keyword={}, selectedId={}, title={}, city={}",
                userId, request.getKeyword(), request.getSelectedId(),
                request.getSelectedTitle(), request.getCity());
    }

    @Override
    public void publish(AiPublishRequest request, Long userId) {
        log.info("灵感发布成功: userId={}, title={}, tag={}, city={}, time={}",
                userId, request.getTitle(), request.getTag(), request.getCity(), LocalDateTime.now());
    }

    /**
     * 未匹配关键词时动态生成灵感
     */
    private List<InspirationCandidate> generateFallback(String keyword) {
        List<InspirationCandidate> result = new ArrayList<>();
        result.add(new InspirationCandidate(1,
                keyword + "创意拓展方案",
                "围绕「" + keyword + "」的多维灵感思路，覆盖玩法、技巧、避坑指南。",
                classifyKeyword(keyword)));
        result.add(new InspirationCandidate(2,
                keyword + "进阶技巧合集",
                "从入门到精通，深度解析「" + keyword + "」的各种实用方法。",
                classifyKeyword(keyword)));
        return result;
    }

    private String classifyKeyword(String keyword) {
        String kw = keyword.toLowerCase();
        if (kw.contains("吃") || kw.contains("鸡") || kw.contains("肉") || kw.contains("菜"))
            return "美食";
        if (kw.contains("运动") || kw.contains("跑") || kw.contains("健身"))
            return "运动";
        if (kw.contains("穿") || kw.contains("搭"))
            return "穿搭";
        if (kw.contains("电影") || kw.contains("影"))
            return "电影";
        if (kw.contains("文") || kw.contains("稿"))
            return "文案";
        return "其他";
    }
}
