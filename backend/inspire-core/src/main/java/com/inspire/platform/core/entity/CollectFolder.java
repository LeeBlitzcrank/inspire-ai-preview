package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collect_folder")
@Schema(description = "收藏文件夹")
public class CollectFolder {
    private Long id;
    private Long userId;
    private String name;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
