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
