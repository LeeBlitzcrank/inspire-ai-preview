/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/dto/InspireUpdateRequest.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.dto;
import com.inspire.platform.common.util.TitleUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data @Schema(description = "更新灵感请求（不传的字段不修改）")
public class InspireUpdateRequest {
    @Schema(description = "标题（最多16个字符）") @Size(max = TitleUtil.MAX_LENGTH) private String title;
    @Schema(description = "正文") private String content;
    @Schema(description = "分类") private String tag;
    @Schema(description = "封面图") private String img;
    @Schema(description = "多图JSON数组") private String images;
    @Schema(description = "0草稿 1发布") private Integer status;
    @Schema(description = "发布城市") private String publishCity;
}
