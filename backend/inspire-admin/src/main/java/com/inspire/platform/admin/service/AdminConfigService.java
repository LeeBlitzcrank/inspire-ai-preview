/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/service/AdminConfigService.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：业务服务接口，定义模块对外能力
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.service;

import com.inspire.platform.admin.entity.AdminConfig;

import java.util.List;
public interface AdminConfigService {
    List<AdminConfig> getAll();
    void update(Integer id, String value);
    void push(String title, String content, String city);
}
