/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/impl/AiHistoryServiceImpl.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.validation.InputValidation;
import com.inspire.platform.core.dto.AiHistorySaveRequest;
import com.inspire.platform.core.entity.UserAiHistory;
import com.inspire.platform.core.mapper.UserAiHistoryMapper;
import com.inspire.platform.core.service.AiHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiHistoryServiceImpl implements AiHistoryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final UserAiHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> list(Long userId, int limit) {
        InputValidation.requirePositive(userId, "用户");
        InputValidation.requirePage(1, limit);
        int safeLimit = limit;
        return historyMapper.selectList(Wrappers.lambdaQuery(UserAiHistory.class)
                        .eq(UserAiHistory::getUserId, userId)
                        .eq(UserAiHistory::getDeleted, 0)
                        .orderByDesc(UserAiHistory::getCreateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> save(Long userId, AiHistorySaveRequest request) {
        InputValidation.requirePositive(userId, "用户");
        String keyword = InputValidation.normalizeRequiredText(
                request.getKeyword(), "探索关键词", 100);
        String path = InputValidation.normalizeOptionalText(request.getPath(), "探索路径", 1000);
        String cacheKey = InputValidation.normalizeOptionalText(request.getCacheKey(), "缓存标识", 600);
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(request.getResult());
        } catch (Exception e) {
            throw new BusinessException(400, "探索结果格式错误");
        }
        if (InputValidation.codePointCount(resultJson) > 100_000) {
            throw new BusinessException(400, "探索结果数据过大");
        }
        UserAiHistory history = new UserAiHistory();
        history.setId(InspireServiceImpl.nextId());
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setPath(path == null ? "" : path);
        history.setCacheKey(cacheKey == null ? "" : cacheKey);
        history.setSelectedIndex(-1);
        history.setSelectedTitle("");
        history.setStatus("generated");
        history.setResultJson(resultJson);
        LocalDateTime now = LocalDateTime.now(ZONE);
        history.setCreateTime(now);
        history.setUpdateTime(now);
        history.setDeleted(0);
        historyMapper.insert(history);
        return toMap(history);
    }

    @Override
    @Transactional
    public void selectVariant(Long userId, Long id, Integer selectedIndex, String selectedTitle) {
        InputValidation.requirePositive(userId, "用户");
        InputValidation.requirePositive(id, "历史记录");
        String safeTitle = InputValidation.normalizeOptionalText(selectedTitle, "标题", 16);
        UserAiHistory history = historyMapper.selectOne(Wrappers.lambdaQuery(UserAiHistory.class)
                .eq(UserAiHistory::getId, id)
                .eq(UserAiHistory::getUserId, userId)
                .eq(UserAiHistory::getDeleted, 0));
        if (history == null) return;
        history.setSelectedIndex(selectedIndex == null ? -1 : selectedIndex);
        history.setSelectedTitle(safeTitle == null ? "" : safeTitle);
        history.setStatus("selected");
        history.setUpdateTime(LocalDateTime.now(ZONE));
        historyMapper.updateById(history);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        InputValidation.requirePositive(userId, "用户");
        InputValidation.requirePositive(id, "历史记录");
        historyMapper.delete(Wrappers.lambdaQuery(UserAiHistory.class)
                .eq(UserAiHistory::getId, id)
                .eq(UserAiHistory::getUserId, userId));
    }

    private Map<String, Object> toMap(UserAiHistory history) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(history.getId()));
        item.put("keyword", history.getKeyword());
        item.put("path", history.getPath());
        item.put("cacheKey", history.getCacheKey());
        item.put("selectedIndex", history.getSelectedIndex());
        item.put("selectedTitle", history.getSelectedTitle());
        item.put("status", history.getStatus());
        item.put("createTime", history.getCreateTime());
        try {
            item.put("result", objectMapper.readValue(
                    history.getResultJson() == null ? "{}" : history.getResultJson(),
                    new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            item.put("result", Map.of());
        }
        return item;
    }
}
