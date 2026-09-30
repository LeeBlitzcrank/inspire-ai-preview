/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/util/TextFilter.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：请求过滤组件，处理进入业务前或响应后的通用逻辑
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容审核过滤器（敏感词检测）
 */
public class TextFilter {

    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
        "赌博", "赌场", "博彩", "色情", "裸聊", "约炮", "迷药", "枪支", "弹药",
        "毒品", "冰毒", "海洛因", "传销", "诈骗", "洗钱", "高利贷", "校园贷",
        "代孕", "卖卵", "器官买卖", "管制刀具", "假钞", "发票", "走私",
        "暴力", "恐怖", "爆炸", "自杀", "邪教", "反动", "分裂"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");

    public static String check(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                return "包含敏感词: " + word;
            }
        }
        if (PHONE_PATTERN.matcher(text).find()) {
            return "包含手机号，需审核";
        }
        if (URL_PATTERN.matcher(text).find()) {
            return "包含外部链接，需审核";
        }
        return null;
    }

    public static boolean hasSensitiveContent(String text) {
        return check(text) != null;
    }
}
