package com.inspire.platform.core.service;

import com.inspire.platform.core.dto.*;
import com.inspire.platform.core.entity.InspireMain;

import java.util.List;

public interface InspireService {

    /** 公开列表（分页+分类） */
    List<InspireVO> listPublic(InspirePageQuery query, Long loginUserId);
    List<InspireVO> recommend(Long userId, int page, int size);

    /** 公开详情 */
    InspireVO getDetail(Long id, Long loginUserId);

    /** 我的发布 */
    List<InspireVO> listMyPublished(Long userId);

    /** 我的草稿 */
    List<InspireVO> listMyDrafts(Long userId);

    /** 创建 */
    InspireMain create(InspireCreateRequest req, Long userId);

    /** 更新 */
    InspireMain update(Long id, InspireUpdateRequest req, Long userId);

    /** 删除 */
    void deleteById(Long id, Long userId);

    /** 收藏 */
    void collect(Long userId, Long inspireId);
    void uncollect(Long userId, Long inspireId);

    /** 点赞 */
    void like(Long userId, Long inspireId);
    void unlike(Long userId, Long inspireId);

    /** 分享 */
    void share(Long userId, Long inspireId);    List<InspireVO> listMyCollects(Long userId);
}
