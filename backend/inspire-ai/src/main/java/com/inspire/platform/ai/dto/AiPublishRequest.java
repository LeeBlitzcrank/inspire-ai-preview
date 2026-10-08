/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiPublishRequest.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Data
@Schema(description = "发布灵感请求")
public class AiPublishRequest {

    @Schema(description = "灵感标题", example = "鸡腿的五种神仙吃法，一周不重样")
    @NotBlank(message = "标题不能为空")
    @Size(max = TITLE_MAX, message = "标题不能超过16个字符")
    private String title;

    @Schema(description = "灵感正文", example = "详细描述各种鸡腿的做法...")
    @NotBlank(message = "内容不能为空")
    @Size(max = CONTENT_MAX, message = "正文不能超过20000个字符")
    private String content;

    @Schema(description = "灵感分类", example = "美食")
    @NotBlank(message = "分类不能为空")
    @Size(max = TAG_MAX, message = "分类名称过长")
    private String tag;

    @Schema(description = "封面图URL", example = "https://picsum.photos/id/102/300/160")
    @Size(max = MEDIA_URL_MAX, message = "封面图地址过长")
    private String img;

    @Schema(description = "发布城市", example = "长沙")
    @Size(max = CITY_MAX, message = "城市名称过长")
    private String city;
}
