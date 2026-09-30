/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/impl/AdminConfigServiceImpl.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.entity.AdminConfig;
import com.inspire.platform.admin.mapper.AdminConfigMapper;
import com.inspire.platform.admin.service.AdminConfigService;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j @Service @RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {
    private final AdminConfigMapper configMapper;

    @Override
    public List<AdminConfig> getAll() {
        return configMapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public void update(Integer id, String value) {
        AdminConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BusinessException("配置不存在");
        }
        cfg.setConfigValue(value);
        configMapper.updateById(cfg);
        log.info("配置更新: key={}, value={}", cfg.getConfigKey(), value);
    }

    @Override
    public void push(String title, String content, String city) {
        log.info("手动推送: title={}, content={}, city={}", title, content, city);
    }
}
