package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.entity.MessageConversation;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final MessageConversationMapper conversationMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override @Transactional
    public Message sendMessage(Long fromUserId, Long toUserId, String content) {
        if (fromUserId.equals(toUserId)) throw new RuntimeException("不能给自己发消息");
        
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
            conversationMapper.insert(conv);
        }
        
        Message msg = new Message();
        msg.setId(nextId());
        msg.setConversationId(conv.getId());
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setContent(content);
        messageMapper.insert(msg);
        
        // 更新会话
        conv.setLastContent(content);
        conv.setLastTime(msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now());
        if (fromUserId == uid1) {
            conv.setUnreadUser2(conv.getUnreadUser2() + 1);
        } else {
            conv.setUnreadUser1(conv.getUnreadUser1() + 1);
        }
        conversationMapper.updateById(conv);
        
        log.info("私信发送: from={}, to={}, content={}", fromUserId, toUserId, content);
        return msg;
    }

    @Override
    public List<MessageConversation> getConversations(Long userId) {
        List<MessageConversation> list = conversationMapper.selectList(Wrappers.lambdaQuery(MessageConversation.class)
                .and(w -> w.eq(MessageConversation::getUser1Id, userId)
                        .or().eq(MessageConversation::getUser2Id, userId))
                .orderByDesc(MessageConversation::getLastTime));
        // 填充对方用户信息
        for (MessageConversation c : list) {
            Long otherId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            try {
                String nickname = jdbcTemplate.queryForObject(
                    "SELECT nickname FROM user WHERE id=?", String.class, otherId);
                String username = jdbcTemplate.queryForObject(
                    "SELECT username FROM user WHERE id=?", String.class, otherId);
                c.setTargetNickname(nickname);
                c.setTargetUsername(username);
            } catch (Exception ignored) {}
        }
        return list;
    }

    @Override
    public List<Message> getMessages(Long userId, Long conversationId, int page, int size) {
        return messageMapper.selectList(Wrappers.lambdaQuery(Message.class)
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + size + " OFFSET " + ((page - 1) * size)));
    }

    @Override @Transactional
    public int markAsRead(Long userId, Long conversationId) {
        MessageConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) return 0;
        if (conv.getUser1Id().equals(userId)) {
            conv.setUnreadUser1(0);
        } else {
            conv.setUnreadUser2(0);
        }
        conversationMapper.updateById(conv);
        return 1;
    }

    @Override
    public int unreadCount(Long userId) {
        List<MessageConversation> list = conversationMapper.selectList(Wrappers.lambdaQuery(MessageConversation.class)
                .and(w -> w.eq(MessageConversation::getUser1Id, userId)
                        .or().eq(MessageConversation::getUser2Id, userId)));
        int count = 0;
        for (MessageConversation c : list) {
            if (c.getUser1Id().equals(userId)) count += c.getUnreadUser1();
            else count += c.getUnreadUser2();
        }
        return count;
    }

    @Override @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        MessageConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) return;
        // 允许会话中的任一方删除
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId))
            throw new RuntimeException("无权删除此会话");
        messageMapper.delete(Wrappers.lambdaQuery(Message.class)
                .eq(Message::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
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
        return conv;
    }


    @Override @Transactional
    public void deleteAllConversations(Long userId) {
        List<MessageConversation> list = conversationMapper.selectList(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(MessageConversation.class)
                .and(w -> w.eq(MessageConversation::getUser1Id, userId)
                        .or().eq(MessageConversation::getUser2Id, userId)));
        for (MessageConversation c : list) {
            messageMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(Message.class)
                    .eq(Message::getConversationId, c.getId()));
            conversationMapper.deleteById(c.getId());
        }
        log.info("清空所有会话: userId={}, count={}", userId, list.size());
    }

}