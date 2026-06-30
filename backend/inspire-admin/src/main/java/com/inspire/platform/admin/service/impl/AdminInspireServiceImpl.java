package com.inspire.platform.admin.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.admin.dto.InspireAdminListVO;
import com.inspire.platform.admin.mapper.InspireMainMapper;
import com.inspire.platform.admin.mapper.InspireMainRow;
import com.inspire.platform.admin.service.AdminInspireService;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AdminInspireServiceImpl implements AdminInspireService {
    private final InspireMainMapper inspireMainMapper;
    @Override
    public Map<String, Object> list(String keyword, String tag, Integer status, int page, int size) {
        LambdaQueryWrapper<InspireMainRow> w = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isEmpty())
            w.like(InspireMainRow::getTitle, keyword);
        if (tag != null && !tag.isEmpty())
            w.eq(InspireMainRow::getTag, tag);
        if (status != null)
            w.eq(InspireMainRow::getStatus, status);
        w.orderByDesc(InspireMainRow::getCreateTime);
        long total = inspireMainMapper.selectCount(w);
        w.last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        List<InspireAdminListVO> list = inspireMainMapper.selectList(w).stream().map(m -> {
            InspireAdminListVO vo = new InspireAdminListVO();
            vo.setId(m.getId()); vo.setTitle(m.getTitle()); vo.setTag(m.getTag());
            vo.setStatus(m.getStatus()); vo.setViewCount(m.getViewCount());
            vo.setLikeCount(m.getLikeCount()); vo.setCollectCount(m.getCollectCount());
            vo.setHeat(m.getHeat()); vo.setPublishCity(m.getPublishCity());
            vo.setDeleted(m.getDeleted()); vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("list", list); result.put("total", total);
        result.put("page", page); result.put("size", size);
        return result;
    }
    @Override
    public void block(Long id) {
        InspireMainRow row = inspireMainMapper.selectById(id);
        if (row == null) throw new BusinessException("灵感不存在，ID=" + id);
        inspireMainMapper.update(null, Wrappers.lambdaUpdate(InspireMainRow.class)
                .set(InspireMainRow::getDeleted, 2).eq(InspireMainRow::getId, id));
    }
    @Override
    public void unblock(Long id) {
        InspireMainRow row = inspireMainMapper.selectById(id);
        if (row == null) throw new BusinessException("灵感不存在，ID=" + id);
        inspireMainMapper.update(null, Wrappers.lambdaUpdate(InspireMainRow.class)
                .set(InspireMainRow::getDeleted, 0).eq(InspireMainRow::getId, id));
    }
}
