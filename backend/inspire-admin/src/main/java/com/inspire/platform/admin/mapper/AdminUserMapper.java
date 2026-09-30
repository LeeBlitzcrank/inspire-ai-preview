/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/mapper/AdminUserMapper.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：数据访问接口，负责数据库读写映射
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.admin.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {}
