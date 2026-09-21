package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 探索词云词条，后台可增删改，前台创建页读取。
 */
@Data
@TableName("sys_word_cloud")
public class WordCloud {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String word;

    /** 权重，用于控制字号大小 */
    private Integer weight;

    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
