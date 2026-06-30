package com.inspire.platform.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("admin_config") @Schema(description = "运营配置")
public class AdminConfig {
    @Schema(description = "配置ID") @TableId(type = IdType.AUTO) private Integer id;
    @Schema(description = "配置key", example = "recommend.hot_weight") private String configKey;
    @Schema(description = "配置值", example = "0.6") private String configValue;
    @Schema(description = "配置说明") @TableField("`desc`")
    private String desc;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
