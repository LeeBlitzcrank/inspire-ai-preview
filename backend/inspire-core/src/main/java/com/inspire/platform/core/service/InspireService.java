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

    /** 我的发布（分页） */
    PageResult<InspireVO> listMyPublished(Long userId, int page, int size);

    /** 我的草稿（分页） */
    PageResult<InspireVO> listMyDrafts(Long userId, int page, int size);

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
    void share(Long userId, Long inspireId);

    /** 我的收藏（分页） */
    PageResult<InspireVO> listMyCollects(Long userId, int page, int size);

    /** 版本历史列表 */
    java.util.List<java.util.Map<String, Object>> listVersions(Long inspireId);

    /** 版本详情 */
    java.util.Map<String, Object> getVersion(Long versionId);

    /** 收藏文件夹 */
    List<com.inspire.platform.core.entity.CollectFolder> getCollectFolders(Long userId);
    com.inspire.platform.core.entity.CollectFolder createCollectFolder(Long userId, String name, String icon);
    void deleteCollectFolder(Long userId, Long folderId);
    void renameCollectFolder(Long userId, Long folderId, String name);
    void collectToFolder(Long userId, Long inspireId, Long folderId);
    void moveCollectToFolder(Long userId, Long inspireId, Long folderId);
    List<com.inspire.platform.core.dto.InspireVO> listCollectsByFolder(Long userId, Long folderId);
    java.util.List<java.util.Map<String, Object>> getHotTags();
}
