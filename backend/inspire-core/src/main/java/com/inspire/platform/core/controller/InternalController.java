/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/controller/InternalController.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.common.validation.InputValidation;
import com.inspire.platform.core.dto.AiCallLogRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@Tag(name = "内部接口")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final com.inspire.platform.core.mapper.AiCallLogMapper aiCallLogMapper;

    @Operation(summary = "记录AI调用", hidden = true)
    @PostMapping("/ai/call")
    public Result<Void> recordAiCall(@Valid @RequestBody AiCallLogRequest request) {
        try {
            String keyword = InputValidation.normalizeOptionalText(
                    request.getKeyword(), "AI调用关键词", 100);
            Long userId = request.getUserId();
            com.inspire.platform.core.entity.AiCallLog log = new com.inspire.platform.core.entity.AiCallLog();
  log.setId(com.inspire.platform.core.service.impl.InspireServiceImpl.nextId());
            log.setCallDate(LocalDate.now());
            log.setKeyword(keyword == null ? "" : keyword);
            log.setUserId(userId == null ? 0 : userId);
            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("记录AI调用失败: {}", e.getMessage());
        }
        return Result.success(null);
    }
}
