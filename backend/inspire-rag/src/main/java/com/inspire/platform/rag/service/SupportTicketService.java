/**
 * 文件：backend/inspire-rag/src/main/java/com/inspire/platform/rag/service/SupportTicketService.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：业务服务实现，承载核心业务流程、事务和依赖编排
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.rag.service;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.rag.model.SupportModels.SupportTicketAttachment;
import com.inspire.platform.rag.model.SupportModels.SupportTicketCreated;
import com.inspire.platform.rag.model.SupportModels.SupportTicketMessageView;
import com.inspire.platform.rag.model.SupportModels.SupportTicketView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public SupportTicketCreated create(String rawIssue, String rawContact, Long userId,
                                       List<SupportTicketAttachment> rawAttachments) {
        String issue = normalize(rawIssue, 1000);
        if (!StringUtils.hasText(issue)) {
            throw new BusinessException(400, "请填写需要人工处理的问题");
        }
        String contact = normalize(rawContact, 200);
        String ticketNo = "ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = sha256(accessToken);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO support_ticket
                        (ticket_no,user_id,contact_value,issue,status,priority,source,access_token_hash)
                    VALUES (?,?,?,?, 'PENDING', 1, 'ai_support', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ticketNo);
            if (userId == null) ps.setNull(2, java.sql.Types.BIGINT);
            else ps.setLong(2, userId);
            ps.setString(3, contact);
            ps.setString(4, issue);
            ps.setString(5, tokenHash);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException(500, "工单创建失败");
        }
        jdbcTemplate.update("""
                INSERT INTO support_ticket_message
                    (ticket_id,sender_type,sender_id,content)
                VALUES (?, 'USER', ?, ?)
                """, key.longValue(), userId, issue);
        insertAttachments(key.longValue(), rawAttachments);
        return new SupportTicketCreated(
                ticketNo,
                accessToken,
                "PENDING",
                LocalDateTime.now().format(TIME_FORMAT)
        );
    }

    public SupportTicketView get(String ticketNo, String accessToken, Long userId) {
        TicketRow row = find(ticketNo);
        if (row == null) {
            throw new BusinessException(404, "工单不存在");
        }
        boolean owner = userId != null && row.userId() != null && row.userId().equals(userId);
        boolean tokenValid = StringUtils.hasText(accessToken)
                && row.accessTokenHash().equals(sha256(accessToken));
        if (!owner && !tokenValid) {
            throw new BusinessException(403, "无权查看该工单");
        }
        List<SupportTicketMessageView> messages = jdbcTemplate.query("""
                SELECT sender_type,content,create_time
                FROM support_ticket_message
                WHERE ticket_id = ?
                ORDER BY create_time ASC, id ASC
                """, (rs, rowNum) -> new SupportTicketMessageView(
                rs.getString("sender_type"),
                rs.getString("content"),
                format(rs.getTimestamp("create_time").toLocalDateTime())
        ), row.id());
        List<SupportTicketAttachment> attachments = jdbcTemplate.query("""
                SELECT file_url,thumb_url,file_type,original_name,duration
                FROM support_ticket_attachment
                WHERE ticket_id = ?
                ORDER BY sort_order ASC, id ASC
                """, (rs, rowNum) -> new SupportTicketAttachment(
                rs.getString("file_url"),
                rs.getString("thumb_url"),
                rs.getString("file_type"),
                rs.getString("original_name"),
                rs.getString("duration")
        ), row.id());
        return new SupportTicketView(
                row.ticketNo(),
                row.status(),
                row.issue(),
                row.contactValue(),
                row.lastReply(),
                null,
                format(row.createTime()),
                format(row.updateTime()),
                messages,
                attachments
        );
    }

    private void insertAttachments(long ticketId, List<SupportTicketAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return;
        int limit = Math.min(attachments.size(), 5);
        for (int i = 0; i < limit; i++) {
            SupportTicketAttachment item = attachments.get(i);
            if (item == null || !safeUploadUrl(item.url())) continue;
            jdbcTemplate.update("""
                    INSERT INTO support_ticket_attachment
                        (ticket_id,file_url,thumb_url,file_type,original_name,duration,sort_order)
                    VALUES (?,?,?,?,?,?,?)
                    """,
                    ticketId,
                    truncate(item.url(), 1000),
                    truncate(safeUploadUrl(item.thumbUrl()) ? item.thumbUrl() : "", 1000),
                    normalizeFileType(item.fileType()),
                    truncate(item.originalName(), 255),
                    truncate(item.duration(), 20),
                    i
            );
        }
    }

    private boolean safeUploadUrl(String value) {
        if (!StringUtils.hasText(value)) return false;
        String url = value.trim();
        return url.startsWith("/uploads/")
                || url.startsWith("/api/file/view?key=upload/")
                || url.startsWith("https://img.20sherry.com/upload/");
    }

    private String normalizeFileType(String value) {
        String type = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "video".equals(type) ? "video" : "image";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        String text = value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private TicketRow find(String ticketNo) {
        if (!StringUtils.hasText(ticketNo) || ticketNo.length() > 32) return null;
        return jdbcTemplate.query("""
                SELECT id,ticket_no,user_id,contact_value,issue,status,last_reply,
                       access_token_hash,create_time,update_time
                FROM support_ticket
                WHERE ticket_no = ?
                """, rs -> rs.next()
                ? new TicketRow(
                        rs.getLong("id"),
                        rs.getString("ticket_no"),
                        (Long) rs.getObject("user_id"),
                        rs.getString("contact_value"),
                        rs.getString("issue"),
                        rs.getString("status"),
                        rs.getString("last_reply"),
                        rs.getString("access_token_hash"),
                        rs.getTimestamp("create_time").toLocalDateTime(),
                        rs.getTimestamp("update_time").toLocalDateTime())
                : null, ticketNo);
    }

    private String normalize(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("工单访问令牌哈希失败", e);
        }
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(TIME_FORMAT);
    }

    private record TicketRow(
            long id,
            String ticketNo,
            Long userId,
            String contactValue,
            String issue,
            String status,
            String lastReply,
            String accessTokenHash,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }
}
