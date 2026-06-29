package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI生成的灵感候选卡片")
public class InspirationCandidate {

    @Schema(description = "候选ID（1或2）", example = "1")
    private Integer id;

    @Schema(description = "灵感标题", example = "鸡腿的五种神仙吃法")
    private String title;

    @Schema(description = "灵感一句话简介", example = "外酥里嫩，厨房小白也能轻松搞定！")
    private String summary;

    @Schema(description = "灵感分类", example = "美食")
    private String tag;
}
