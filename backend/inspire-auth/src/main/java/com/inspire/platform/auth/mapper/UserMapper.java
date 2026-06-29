package com.inspire.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
