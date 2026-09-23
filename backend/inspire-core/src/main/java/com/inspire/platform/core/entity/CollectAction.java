package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collect")
@Schema(description = "用户收藏记录（collect 按 user_id HASH 分区）")
public class CollectAction {
    private Long id;
    private Long userId;
    private Long inspireId;
    private Long folderId;
    private LocalDateTime createTime;
}
