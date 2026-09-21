package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_word_cloud")
@Schema(description = "AI探索词云词条")
public class WordCloud {

    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "词条内容")
    private String word;

    @Schema(description = "权重，用于控制字号")
    private Integer weight;

    @Schema(description = "排序，越小越靠前")
    private Integer sortOrder;

    @Schema(description = "1启用 0停用")
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
