package com.inspire.platform.ai.service;

import com.inspire.platform.ai.dto.*;

public interface AiService {

    /**
     * 根据关键词生成2条灵感候选
     * PRD 3.模块2：调用Codex AI返回2条灵感标题卡片
     */
    AiGenerateResponse generate(AiGenerateRequest request);

    /**
     * 用户选中一条灵感，上报行为数据
     * PRD 3.模块2：自动上报userId、关键词、选中灵感、城市、操作时间
     */
    void select(AiSelectRequest request, Long userId);

    /**
     * 发布灵感至广场
     * PRD 3.模块2：填写正文后公开发布
     */
    void publish(AiPublishRequest request, Long userId);
}
