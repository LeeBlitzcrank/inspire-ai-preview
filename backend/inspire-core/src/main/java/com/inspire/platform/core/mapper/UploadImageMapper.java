package com.inspire.platform.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspire.platform.core.entity.UploadImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图片上传元数据 Mapper
 */
@Mapper
public interface UploadImageMapper extends BaseMapper<UploadImage> {
}
