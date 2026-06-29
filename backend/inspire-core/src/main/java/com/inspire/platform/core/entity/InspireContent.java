package com.inspire.platform.core.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("inspire_content")
@Schema(description = "灵感正文（冷热分离冷数据）")
public class InspireContent {
    @Schema(description = "关联灵感ID") @TableId private Long inspireId;
    @Schema(description = "灵感完整正文") private String content;
}
