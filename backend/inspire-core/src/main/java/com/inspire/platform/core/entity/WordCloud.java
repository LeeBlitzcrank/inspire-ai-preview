/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/WordCloud.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
