package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        int safeLimit = Math.max(1, Math.min(limit, 50));
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
    public Map<String, Object> save(Long userId, Map<String, Object> body) {
        UserAiHistory history = new UserAiHistory();
        history.setId(InspireServiceImpl.nextId());
        history.setUserId(userId);
        history.setKeyword(String.valueOf(body.getOrDefault("keyword", "")));
        history.setPath(String.valueOf(body.getOrDefault("path", "")));
        history.setCacheKey(String.valueOf(body.getOrDefault("cacheKey", "")));
        history.setSelectedIndex(-1);
        history.setSelectedTitle("");
        history.setStatus("generated");
        try {
            history.setResultJson(objectMapper.writeValueAsString(body.get("result")));
        } catch (Exception e) {
            throw new IllegalArgumentException("探索结果格式错误");
        }
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
        UserAiHistory history = historyMapper.selectOne(Wrappers.lambdaQuery(UserAiHistory.class)
                .eq(UserAiHistory::getId, id)
                .eq(UserAiHistory::getUserId, userId)
                .eq(UserAiHistory::getDeleted, 0));
        if (history == null) return;
        history.setSelectedIndex(selectedIndex == null ? -1 : selectedIndex);
        history.setSelectedTitle(selectedTitle == null ? "" : selectedTitle);
        history.setStatus("selected");
        history.setUpdateTime(LocalDateTime.now(ZONE));
        historyMapper.updateById(history);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
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
