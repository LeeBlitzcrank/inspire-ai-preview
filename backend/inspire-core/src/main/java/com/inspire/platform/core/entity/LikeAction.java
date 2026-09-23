package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_like")
@Schema(description = "用户点赞记录（user_like 按 user_id HASH 分区）")
public class LikeAction {
    private Long id;
    @Schema(description = "点赞用户ID") private Long userId;
    private Long inspireId;
    private LocalDateTime createTime;
}
