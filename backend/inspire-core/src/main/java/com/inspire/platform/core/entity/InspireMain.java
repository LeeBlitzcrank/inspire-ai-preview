package com.inspire.platform.core.entity;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inspire_main")
@Schema(description = "灵感主表")
public class InspireMain {
    @Schema(description = "灵感ID") @TableId(type = IdType.INPUT) private Long id;
    @Schema(description = "灵感标题", example = "鸡腿的五种神仙吃法") private String title;
    @Schema(description = "封面图") private String img;
    @Schema(description = "分类", example = "美食") private String tag;
    @Schema(description = "发布人ID") private Long userId;
    @Schema(description = "0草稿 1已发布") private Integer status;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "点赞数") private Integer likeCount;
    @Schema(description = "收藏数") private Integer collectCount;
    @Schema(description = "综合热度") private Integer heat;
    @Schema(description = "发布城市") private String publishCity;
    @Schema(description = "发布时间") @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @Schema(description = "更新时间") @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @Schema(hidden = true) private Integer deleted;
    @Schema(hidden = true) private String extJson;
}
