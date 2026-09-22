package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collect_folder")
@Schema(description = "收藏文件夹")
public class CollectFolder {
    // 雪花 ID 超出 JS 安全整数范围（2^53），必须以字符串下发，
    // 否则前端拿到的 6 个 folderId 会全部被舍入成同一个数，
    // 表现为：每个文件夹条数相同、选中一个所有卡片都变选中态。
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String name;
    private String icon;
    private Integer sortOrder;
    @TableField(exist = false)
    private Integer count;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
