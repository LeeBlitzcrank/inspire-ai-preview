package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.core.entity.ConversationMember;
import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;
import com.inspire.platform.core.mapper.ConversationMemberMapper;
import com.inspire.platform.core.mapper.MessageConversationMapper;
import com.inspire.platform.core.mapper.MessageMapper;
import com.inspire.platform.core.service.MessageService;
import static com.inspire.platform.core.service.impl.InspireServiceImpl.nextId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** 统一使用北京时间写入时间字段，避免依赖 MySQL 的 CURRENT_TIMESTAMP（容器时区可能是 UTC） */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final MessageMapper messageMapper;
    private final MessageConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override @Transactional
    public Message sendMessage(Long fromUserId, Long toUserId, String content) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能给自己发消息");
        }
        
        // 确保 user1_id < user2_id 用于唯一约束
        long uid1 = Math.min(fromUserId, toUserId);
        long uid2 = Math.max(fromUserId, toUserId);
        
        MessageConversation conv = conversationMapper.selectOne(Wrappers.lambdaQuery(MessageConversation.class)
                .eq(MessageConversation::getUser1Id, uid1)
                .eq(MessageConversation::getUser2Id, uid2));
        
        if (conv == null) {
            conv = new MessageConversation();
            conv.setId(nextId());
            conv.setUser1Id(uid1);
            conv.setUser2Id(uid2);
            conv.setUnreadUser1(0);
            conv.setUnreadUser2(0);
            LocalDateTime created = LocalDateTime.now(ZONE);
            conv.setCreateTime(created);
            conv.setUpdateTime(created);
            conversationMapper.insert(conv);
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        Message msg = new Message();
        msg.setId(nextId());
        msg.setConversationId(conv.getId());
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        msg.setCreateTime(now);
        messageMapper.insert(msg);
        
        // 更新会话
        conv.setLastContent(content);
        conv.setLastTime(now);
        conv.setUpdateTime(now);
        conversationMapper.updateById(conv);
        ensureConversationMembers(conv, now);
        jdbcTemplate.update(
                "UPDATE conversation_member SET unread_count = unread_count + 1, last_time = ?, update_time = ? "
                        + "WHERE conversation_id = ? AND user_id = ?",
                now, now, conv.getId(), toUserId);
        
        log.info("私信发送: from={}, to={}, content={}", fromUserId, toUserId, content);
        return msg;
    }

    @Override
    public List<MessageConversation> getConversations(Long userId) {
        return jdbcTemplate.query(
                "SELECT c.id, c.user1_id, c.user2_id, c.last_content, cm.last_time, cm.unread_count, "
                        + "u.id AS target_id, u.nickname, u.username "
                        + "FROM conversation_member cm "
                        + "JOIN message_conversation c ON c.id = cm.conversation_id "
                        + "JOIN user u ON u.id = CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END "
                        + "WHERE cm.user_id = ? ORDER BY cm.last_time DESC",
                (rs, n) -> {
                    MessageConversation c = new MessageConversation();
                    c.setId(rs.getLong("id"));
                    c.setUser1Id(rs.getLong("user1_id"));
                    c.setUser2Id(rs.getLong("user2_id"));
                    c.setLastContent(rs.getString("last_content"));
                    c.setLastTime(rs.getTimestamp("last_time") != null
                            ? rs.getTimestamp("last_time").toLocalDateTime() : null);
                    c.setUnreadUser1(rs.getInt("unread_count"));
                    c.setUnreadUser2(rs.getInt("unread_count"));
                    c.setTargetNickname(rs.getString("nickname"));
                    c.setTargetUsername(rs.getString("username"));
                    return c;
                }, userId, userId);
    }

    @Override
    public List<Message> getMessages(Long userId, Long conversationId, int page, int size) {
        Integer member = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conversation_member WHERE conversation_id = ? AND user_id = ?",
                Integer.class, conversationId, userId);
        if (member == null || member == 0) {
            throw new RuntimeException("无权查看此会话");
        }
        return messageMapper.selectList(Wrappers.lambdaQuery(Message.class)
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + size + " OFFSET " + ((page - 1) * size)));
    }

    @Override @Transactional
    public int markAsRead(Long userId, Long conversationId) {
        return jdbcTemplate.update(
                "UPDATE conversation_member SET unread_count = 0, update_time = ? "
                        + "WHERE conversation_id = ? AND user_id = ?",
                LocalDateTime.now(ZONE), conversationId, userId);
    }

    @Override
    public int unreadCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(unread_count), 0) FROM conversation_member WHERE user_id = ?",
                Integer.class, userId);
        return count == null ? 0 : count;
    }

    @Override @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Integer member = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conversation_member WHERE conversation_id = ? AND user_id = ?",
                Integer.class, conversationId, userId);
        if (member == null || member == 0) {
            throw new RuntimeException("无权删除此会话");
        }
        messageMapper.delete(Wrappers.lambdaQuery(Message.class)
                .eq(Message::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
        jdbcTemplate.update("DELETE FROM conversation_member WHERE conversation_id = ?", conversationId);
        log.info("删除会话: conversationId={}, userId={}", conversationId, userId);
    }


    @Override
    public MessageConversation startConversation(Long userId, Long toUserId) {
        long uid1 = Math.min(userId, toUserId);
        long uid2 = Math.max(userId, toUserId);
        MessageConversation conv = conversationMapper.selectOne(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(MessageConversation.class)
                .eq(MessageConversation::getUser1Id, uid1)
                .eq(MessageConversation::getUser2Id, uid2));
        if (conv == null) {
            conv = new MessageConversation();
            conv.setId(InspireServiceImpl.nextId());
            conv.setUser1Id(uid1);
            conv.setUser2Id(uid2);
            conv.setUnreadUser1(0);
            conv.setUnreadUser2(0);
            conversationMapper.insert(conv);
        }
        ensureConversationMembers(conv, LocalDateTime.now(ZONE));
        return conv;
    }


    @Override @Transactional
    public void deleteAllConversations(Long userId) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT conversation_id FROM conversation_member WHERE user_id = ?",
                Long.class, userId);
        for (Long id : ids) {
            messageMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(Message.class)
                    .eq(Message::getConversationId, id));
            conversationMapper.deleteById(id);
            jdbcTemplate.update("DELETE FROM conversation_member WHERE conversation_id = ?", id);
        }
        log.info("清空所有会话: userId={}, count={}", userId, ids.size());
    }

    private void ensureConversationMembers(MessageConversation conv, LocalDateTime now) {
        for (Long memberId : List.of(conv.getUser1Id(), conv.getUser2Id())) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM conversation_member WHERE conversation_id = ? AND user_id = ?",
                    Integer.class, conv.getId(), memberId);
            if (exists == null || exists == 0) {
                ConversationMember member = new ConversationMember();
                member.setId(nextId());
                member.setConversationId(conv.getId());
                member.setUserId(memberId);
                member.setUnreadCount(0);
                member.setLastTime(conv.getLastTime() != null ? conv.getLastTime() : now);
                member.setCreateTime(now);
                member.setUpdateTime(now);
                conversationMemberMapper.insert(member);
            } else {
                jdbcTemplate.update(
                        "UPDATE conversation_member SET last_time = ?, update_time = ? "
                                + "WHERE conversation_id = ? AND user_id = ?",
                        conv.getLastTime() != null ? conv.getLastTime() : now, now, conv.getId(), memberId);
            }
        }
    }

}
