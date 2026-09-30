/**
 * 文件：backend/inspire-auth/src/main/java/com/inspire/platform/auth/mapper/UserMapper.java
 * 所属模块：用户认证模块，负责登录、令牌、会话、密码和登录风控
 * 主要职责：数据访问接口，负责数据库读写映射
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
