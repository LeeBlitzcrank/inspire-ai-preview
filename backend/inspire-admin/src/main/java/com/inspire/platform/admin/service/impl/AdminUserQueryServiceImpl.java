/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/impl/AdminUserQueryServiceImpl.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.entity.AdminUserRow;
import com.inspire.platform.admin.mapper.AdminUserRowMapper;
import com.inspire.platform.admin.service.AdminUserQueryService;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.validation.InputValidation;
import com.inspire.platform.common.validation.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service @RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {
    private final AdminUserRowMapper userRowMapper;

    @Override
    public List<AdminUserRow> search(String keyword, int page, int size) {
        InputValidation.requirePage(page, size);
        String safeKeyword = InputValidation.normalizeOptionalText(
                keyword, "搜索关键词", ValidationConstants.AI_KEYWORD_MAX);
        LambdaQueryWrapper<AdminUserRow> w = Wrappers.lambdaQuery();
        if (safeKeyword != null && !safeKeyword.isEmpty()) {
            w.like(AdminUserRow::getUsername, safeKeyword).or()
             .like(AdminUserRow::getEmail, safeKeyword).or()
             .like(AdminUserRow::getNickname, safeKeyword);
        }
        w.eq(AdminUserRow::getDeleted, 0);
        w.orderByDesc(AdminUserRow::getCreateTime);
        w.last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        return userRowMapper.selectList(w);
    }

    @Override
    public AdminUserRow detail(Long id) {
        InputValidation.requirePositive(id, "用户");
        AdminUserRow user = userRowMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public long total() {
        return userRowMapper.selectCount(Wrappers.lambdaQuery(AdminUserRow.class).eq(AdminUserRow::getDeleted, 0));
    }
}
