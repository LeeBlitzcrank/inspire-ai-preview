/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/InspireCreateRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;

import com.inspire.platform.common.util.TitleUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.*;

@Data @Schema(description = "创建灵感请求")
public class InspireCreateRequest {
    @Schema(description = "标题（最多16个字符）")
    @NotBlank(message = "标题不能为空")
    @Size(max = TitleUtil.MAX_LENGTH, message = "标题不能超过16个字符")
    private String title;

    @Schema(description = "正文")
    @NotBlank(message = "正文不能为空")
    @Size(max = CONTENT_MAX, message = "正文不能超过20000个字符")
    private String content;

    @Schema(description = "分类", example = "美食")
    @NotBlank(message = "分类不能为空")
    @Size(max = TAG_MAX, message = "分类名称过长")
    private String tag;

    @Schema(description = "封面图")
    @Size(max = MEDIA_URL_MAX, message = "封面图地址过长")
    private String img;

    @Schema(description = "多图JSON数组")
    @Size(max = IMAGE_JSON_MAX, message = "图片列表数据过大")
    private String images;

    @Schema(description = "0草稿 1发布", example = "1")
    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status;

    @Schema(description = "发布城市", example = "长沙")
    @Size(max = CITY_MAX, message = "城市名称过长")
    private String publishCity;

    @Schema(description = "引用再创作的原灵感ID")
    @Positive(message = "引用的灵感ID不正确")
    private Long quoteInspireId;
}
