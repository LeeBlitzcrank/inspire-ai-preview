/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/AdminInspireService.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service;

import java.util.Map;
public interface AdminInspireService {
    Map<String, Object> list(String keyword, String tag, Integer status, int page, int size);
    void block(Long id);
    void unblock(Long id);
    void approve(Long id);
    void reject(Long id);
    Map<String, Object> listPending(int page, int size);
}
