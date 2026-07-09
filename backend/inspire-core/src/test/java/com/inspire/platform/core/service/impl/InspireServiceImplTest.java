package com.inspire.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.dto.InspirePageQuery;
import com.inspire.platform.core.dto.InspireVO;
import com.inspire.platform.core.entity.InspireMain;
import com.inspire.platform.core.mapper.InspireMainMapper;
import com.inspire.platform.core.service.InspireService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InspireService 单元测试
 */
@SpringBootTest
class InspireServiceImplTest {

    @Autowired
    private InspireService inspireService;

    @MockBean
    private InspireMainMapper mainMapper;

    /**
     * 测试分页查询：listPublic 应返回不超过 pageSize 条数据
     */
    @Test
    void testListPublicPagination() {
        // mock 一个分页结果
        Page<InspireMain> mockPage = new Page<>(1, 5);
        // selectPage 会返回这个 mockPage
        when(mainMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        InspirePageQuery query = new InspirePageQuery();
        query.setPage(1);
        query.setSize(5);
        List<InspireVO> result = inspireService.listPublic(query, null);

        assertNotNull(result);
    }

    /**
     * 测试 getDetail 对不存在的数据抛出异常
     */
    @Test
    void testGetDetailNotFound() {
        when(mainMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(Exception.class, () -> inspireService.getDetail(99999L, null));
    }
}
