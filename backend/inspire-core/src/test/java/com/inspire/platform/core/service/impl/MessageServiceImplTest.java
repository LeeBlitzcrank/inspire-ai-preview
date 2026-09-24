package com.inspire.platform.core.service.impl;

import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.core.entity.Message;
import com.inspire.platform.core.mapper.ConversationMemberMapper;
import com.inspire.platform.core.mapper.MessageConversationMapper;
import com.inspire.platform.core.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceImplTest {

    @Test
    void rejectsRecallAfterTwoMinutes() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        MessageServiceImpl service = new MessageServiceImpl(
                messageMapper,
                mock(MessageConversationMapper.class),
                mock(ConversationMemberMapper.class),
                mock(JdbcTemplate.class));

        Message message = new Message();
        message.setId(1L);
        message.setConversationId(2L);
        message.setFromUserId(3L);
        message.setCreateTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(3));
        when(messageMapper.selectOne(any())).thenReturn(message);

        assertThrows(BusinessException.class,
                () -> service.recallMessage(3L, 2L, 1L));
    }
}
