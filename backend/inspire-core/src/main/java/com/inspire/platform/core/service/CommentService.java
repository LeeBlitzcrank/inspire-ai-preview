/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/CommentService.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspire.platform.core.dto.CommentCreateRequest;
import com.inspire.platform.core.dto.CommentVO;

public interface CommentService {
    Page<CommentVO> listByInspireId(Long inspireId, Long userId, int page, int size, String sort);
    Page<CommentVO> listReplies(Long inspireId, Long parentId, Long userId, int page, int size, String sort);
    CommentVO create(Long userId, CommentCreateRequest request);
    void deleteById(Long inspireId, Long commentId, Long userId);
    boolean like(Long userId, Long inspireId, Long commentId);
    boolean unlike(Long userId, Long inspireId, Long commentId);
}
