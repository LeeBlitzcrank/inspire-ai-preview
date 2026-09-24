package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.core.dto.InspirePageQuery;
import com.inspire.platform.core.dto.InspireVO;
import com.inspire.platform.core.mapper.*;
import com.inspire.platform.core.service.ContentCacheService;
import com.inspire.platform.core.service.FeedService;
import com.inspire.platform.core.service.NotificationService;
import com.inspire.platform.core.service.ViewCountService;
import com.inspire.platform.core.service.es.EsSyncService;
import com.inspire.platform.mq.producer.MqProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspireServiceImplTest {

    @InjectMocks
    private InspireServiceImpl inspireService;

    @Mock private InspireMainMapper mainMapper;
    @Mock private InspireContentMapper contentMapper;
    @Mock private CollectMapper collectMapper;
    @Mock private CollectFolderMapper collectFolderMapper;
    @Mock private InspireSeriesMapper seriesMapper;
    @Mock private LikeMapper likeMapper;
    @Mock private EsSyncService esSyncService;
    @Mock private NotificationService notificationService;
    @Mock private ContentCacheService contentCacheService;
    @Mock private FeedService feedService;
    @Mock private ViewCountService viewCountService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MqProducer mqProducer;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ObjectMapper objectMapper;

    @Test
    void testListPublicPagination() {
        when(mainMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 5));

        InspirePageQuery query = new InspirePageQuery();
        query.setPage(1);
        query.setSize(5);
        List<InspireVO> result = inspireService.listPublic(query, null);

        assertNotNull(result);
    }

    @Test
    void testGetDetailNotFound() {
        when(mainMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(Exception.class, () -> inspireService.getDetail(99999L, null));
    }
}
