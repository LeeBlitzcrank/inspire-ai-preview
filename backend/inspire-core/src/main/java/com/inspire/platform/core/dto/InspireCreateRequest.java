package com.inspire.platform.core.dto;
import com.inspire.platform.common.util.TitleUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data @Schema(description = "创建灵感请求")
public class InspireCreateRequest {
    @Schema(description = "标题（最多16个字符）") @NotBlank @Size(max = TitleUtil.MAX_LENGTH) private String title;
    @Schema(description = "正文") @NotBlank private String content;
    @Schema(description = "分类", example = "美食") @NotBlank private String tag;
    @Schema(description = "封面图") private String img;
    @Schema(description = "多图JSON数组") private String images;
    @Schema(description = "0草稿 1发布", example = "1") private Integer status;
    @Schema(description = "发布城市", example = "长沙") private String publishCity;
}
