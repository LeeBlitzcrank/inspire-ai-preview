/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/AdminSupportTicketService.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.entity.SupportTicketAdmin;
import com.inspire.platform.admin.entity.SupportTicketAttachmentAdmin;
import com.inspire.platform.admin.entity.SupportTicketMessageAdmin;
import com.inspire.platform.admin.mapper.SupportTicketAdminMapper;
import com.inspire.platform.admin.mapper.SupportTicketAttachmentAdminMapper;
import com.inspire.platform.admin.mapper.SupportTicketMessageAdminMapper;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminSupportTicketService {

    private final SupportTicketAdminMapper ticketMapper;
    private final SupportTicketMessageAdminMapper messageMapper;
    private final SupportTicketAttachmentAdminMapper attachmentMapper;

    public Map<String, Object> list(String status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        var query = Wrappers.<SupportTicketAdmin>lambdaQuery()
                .orderByDesc(SupportTicketAdmin::getUpdateTime)
                .last("LIMIT " + safeSize + " OFFSET " + (safePage - 1) * safeSize);
        if (StringUtils.hasText(status)) {
            query.eq(SupportTicketAdmin::getStatus, status.trim().toUpperCase());
        }
        List<SupportTicketAdmin> list = ticketMapper.selectList(query);
        Long total = ticketMapper.selectCount(StringUtils.hasText(status)
                ? Wrappers.<SupportTicketAdmin>lambdaQuery()
                .eq(SupportTicketAdmin::getStatus, status.trim().toUpperCase())
                : Wrappers.emptyWrapper());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        SupportTicketAdmin ticket = required(id);
        List<SupportTicketMessageAdmin> messages = messageMapper.selectList(
                Wrappers.<SupportTicketMessageAdmin>lambdaQuery()
                        .eq(SupportTicketMessageAdmin::getTicketId, id)
                        .orderByAsc(SupportTicketMessageAdmin::getCreateTime)
                        .orderByAsc(SupportTicketMessageAdmin::getId));
        List<SupportTicketAttachmentAdmin> attachments = attachmentMapper.selectList(
                Wrappers.<SupportTicketAttachmentAdmin>lambdaQuery()
                        .eq(SupportTicketAttachmentAdmin::getTicketId, id)
                        .orderByAsc(SupportTicketAttachmentAdmin::getSortOrder)
                        .orderByAsc(SupportTicketAttachmentAdmin::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket", ticket);
        result.put("messages", messages);
        result.put("attachments", attachments);
        return result;
    }

    @Transactional
    public void claim(Long id, Long adminId) {
        SupportTicketAdmin ticket = required(id);
        if ("CLOSED".equals(ticket.getStatus())) {
            throw new BusinessException(400, "已关闭的工单不能认领");
        }
        ticket.setStatus("PROCESSING");
        ticket.setAssigneeId(adminId);
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void reply(Long id, Long adminId, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(400, "回复内容不能超过2000个字符");
        }
        SupportTicketAdmin ticket = required(id);
        if ("CLOSED".equals(ticket.getStatus())) {
            throw new BusinessException(400, "已关闭的工单不能回复");
        }
        SupportTicketMessageAdmin message = new SupportTicketMessageAdmin();
        message.setTicketId(id);
        message.setSenderType("ADMIN");
        message.setSenderId(adminId);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        ticket.setStatus("REPLIED");
        ticket.setLastReply(content.length() > 500 ? content.substring(0, 500) : content);
        if (ticket.getAssigneeId() == null) ticket.setAssigneeId(adminId);
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void close(Long id, Long adminId) {
        SupportTicketAdmin ticket = required(id);
        ticket.setStatus("CLOSED");
        if (ticket.getAssigneeId() == null) ticket.setAssigneeId(adminId);
        ticket.setClosedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    private SupportTicketAdmin required(Long id) {
        SupportTicketAdmin ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new BusinessException(404, "工单不存在");
        return ticket;
    }
}
