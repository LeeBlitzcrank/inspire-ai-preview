package com.inspire.platform.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;

public interface CommentService {
    Page<CommentVO> listByInspireId(Long inspireId, int page, int size);
    void create(Long userId, CommentCreateRequest request);
    void deleteById(Long commentId, Long userId);
}
