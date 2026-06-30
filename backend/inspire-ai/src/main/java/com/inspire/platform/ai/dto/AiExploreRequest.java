package com.inspire.platform.ai.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data @Schema(description = "AI探索请求")
public class AiExploreRequest {
    @Schema(description = "关键词", example = "鸡腿")
    @NotBlank private String keyword;
    @Schema(description = "当前路径（逗号分隔）", example = "opt1,detail2")
    private String path;
    @Schema(description = "是否刷新（跳过缓存重新生成）", example = "false")
    private boolean refresh;
}
