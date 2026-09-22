package com.inspire.platform.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;

public interface CommentService {
    Page<CommentVO> listByInspireId(Long inspireId, Long userId, int page, int size, String sort);
    void create(Long userId, CommentCreateRequest request);
    void deleteById(Long inspireId, Long commentId, Long userId);
    boolean like(Long userId, Long inspireId, Long commentId);
    boolean unlike(Long userId, Long inspireId, Long commentId);
}
