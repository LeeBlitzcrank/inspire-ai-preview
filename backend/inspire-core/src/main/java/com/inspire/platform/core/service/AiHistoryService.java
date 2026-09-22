package com.inspire.platform.core.service;

import java.util.List;
import java.util.Map;

public interface AiHistoryService {
    List<Map<String, Object>> list(Long userId, int limit);
    Map<String, Object> save(Long userId, Map<String, Object> body);
    void selectVariant(Long userId, Long id, Integer selectedIndex, String selectedTitle);
    void delete(Long userId, Long id);
}
