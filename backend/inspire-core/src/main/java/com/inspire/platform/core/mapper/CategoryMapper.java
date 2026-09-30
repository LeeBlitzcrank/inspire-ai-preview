/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/mapper/CategoryMapper.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：数据访问接口，负责数据库读写映射
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.core.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
