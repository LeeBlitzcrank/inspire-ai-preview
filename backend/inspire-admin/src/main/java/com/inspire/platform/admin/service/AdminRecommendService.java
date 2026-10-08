/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/AdminRecommendService.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.dto.RecommendConfigUpdateRequest;
import com.inspire.platform.admin.dto.RecommendPushRequest;
import com.inspire.platform.admin.entity.RecommendConfig;
import com.inspire.platform.admin.entity.RecommendPush;
import com.inspire.platform.admin.mapper.RecommendConfigMapper;
import com.inspire.platform.admin.mapper.RecommendPushMapper;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminRecommendService {

    private static final Set<String> TARGET_TYPES = Set.of("ALL", "TAG", "NEW_USER");
    private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "PAUSED", "ENDED");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecommendConfigMapper configMapper;
    private final RecommendPushMapper pushMapper;

    public List<RecommendConfig> configList() {
        return configMapper.selectList(Wrappers.<RecommendConfig>lambdaQuery()
                .orderByAsc(RecommendConfig::getConfigKey));
    }

    @Transactional
    public void updateConfigs(List<RecommendConfigUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(400, "配置不能为空");
        }
        for (RecommendConfigUpdateRequest request : requests) {
            RecommendConfig config = configMapper.selectById(request.configKey());
            if (config == null) config = new RecommendConfig();
            config.setConfigKey(request.configKey().trim());
            config.setConfigValue(request.configValue().trim());
            if (configMapper.selectById(config.getConfigKey()) == null) {
                configMapper.insert(config);
            } else {
                configMapper.updateById(config);
            }
        }
    }

    public List<RecommendPush> pushList() {
        return pushMapper.selectWithInspire();
    }

    @Transactional
    public RecommendPush savePush(Long id, RecommendPushRequest request, Long adminId) {
        String targetType = normalize(request.targetType());
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(400, "目标人群类型不支持");
        }
        String status = normalize(request.status());
        if (!STATUSES.contains(status)) {
            throw new BusinessException(400, "推送状态不支持");
        }
        if ("TAG".equals(targetType) && !StringUtils.hasText(request.targetValue())) {
            throw new BusinessException(400, "分类定向必须填写分类名称");
        }
        LocalDateTime startTime = parseTime(request.startTime());
        LocalDateTime endTime = parseTime(request.endTime());
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        RecommendPush push = id == null ? new RecommendPush() : pushMapper.selectById(id);
        if (push == null) throw new BusinessException(404, "推送不存在");
        push.setInspireId(request.inspireId());
        push.setTargetType(targetType);
        push.setTargetValue(request.targetValue() == null ? "" : request.targetValue().trim());
        push.setWeight(request.weight() == null ? BigDecimal.ONE : request.weight());
        push.setStatus(status);
        push.setStartTime(startTime);
        push.setEndTime(endTime);
        push.setReason(request.reason() == null ? "" : request.reason().trim());
        if (id == null) {
            push.setCreatedBy(adminId);
            pushMapper.insert(push);
            return push;
        }
        pushMapper.updateById(push);
        return push;
    }

    @Transactional
    public void changeStatus(Long id, String rawStatus) {
        RecommendPush push = pushMapper.selectById(id);
        if (push == null) throw new BusinessException(404, "推送不存在");
        String status = normalize(rawStatus);
        if (!STATUSES.contains(status)) throw new BusinessException(400, "推送状态不支持");
        push.setStatus(status);
        if ("ACTIVE".equals(status) && push.getStartTime() == null) {
            push.setStartTime(LocalDateTime.now());
        }
        pushMapper.updateById(push);
    }

    public List<Map<String, Object>> pushMetrics() {
        return pushMapper.selectPushMetrics();
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDateTime.parse(value.trim().replace('T', ' '), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
