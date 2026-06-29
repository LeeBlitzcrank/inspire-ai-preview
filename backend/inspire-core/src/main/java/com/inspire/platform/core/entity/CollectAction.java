package com.inspire.platform.core.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collect")
@Schema(description = "用户收藏记录（分表 collect_0~9，按user_id%10路由）")
public class CollectAction {
    private Long id;
    private Long userId;
    @Schema(description = "被收藏的灵感ID") private Long inspireId;
    private LocalDateTime createTime;
}
