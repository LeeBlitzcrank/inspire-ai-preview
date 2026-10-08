package com.inspire.platform.core.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VideoTrimRequest {
    @NotBlank(message = "视频地址不能为空")
    @Size(max = 255, message = "视频地址过长")
    @Pattern(regexp = "^/uploads/[A-Za-z0-9._-]+$", message = "视频地址格式不正确")
    private String url;

    @NotNull(message = "开始时间不能为空")
    @DecimalMin(value = "0", message = "开始时间不能小于0")
    private Double start = 0D;

    @NotNull(message = "保留时长不能为空")
    @DecimalMin(value = "0.1", message = "保留时长必须大于0")
    private Double duration;

    private Boolean keepOriginal = true;
}
