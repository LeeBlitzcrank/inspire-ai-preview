/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/service/AiService.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.service;
import com.inspire.platform.ai.dto.*;

public interface AiService {
    AiGenerateResponse generate(AiGenerateRequest request);
    void select(AiSelectRequest request, Long userId);
    void publish(AiPublishRequest request, Long userId);
    AiExploreResponse explore(AiExploreRequest request);
    AiRewriteResponse rewrite(AiRewriteRequest request);
    AiTitleResponse titles(AiTitleRequest request);
}
