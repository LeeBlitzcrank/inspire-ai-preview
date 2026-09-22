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
