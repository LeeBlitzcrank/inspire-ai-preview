/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/UserAiHistory.java
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

@Data
@TableName("user_ai_history")
public class UserAiHistory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String keyword;
    private String path;
    private String cacheKey;
    private String resultJson;
    private Integer selectedIndex;
    private String selectedTitle;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
