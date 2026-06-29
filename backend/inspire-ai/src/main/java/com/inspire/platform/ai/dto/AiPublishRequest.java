package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发布灵感请求")
public class AiPublishRequest {

    @Schema(description = "灵感标题", example = "鸡腿的五种神仙吃法，一周不重样")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "灵感正文", example = "详细描述各种鸡腿的做法...")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "灵感分类", example = "美食")
    @NotBlank(message = "分类不能为空")
    private String tag;

    @Schema(description = "封面图URL", example = "https://picsum.photos/id/102/300/160")
    private String img;

    @Schema(description = "发布城市", example = "长沙")
    private String city;
}
