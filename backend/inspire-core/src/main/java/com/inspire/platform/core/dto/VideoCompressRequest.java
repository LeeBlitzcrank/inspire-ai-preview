package com.inspire.platform.core.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VideoCompressRequest {
    @NotBlank(message = "视频地址不能为空")
    @Size(max = 255, message = "视频地址过长")
    @Pattern(regexp = "^/uploads/[A-Za-z0-9._-]+$", message = "视频地址格式不正确")
    private String url;

    @Min(value = 18, message = "压缩质量参数不正确")
    @Max(value = 34, message = "压缩质量参数不正确")
    private Integer crf = 28;

    private Boolean keepOriginal = true;
}
