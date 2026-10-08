package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VideoProbeRequest {
    @NotBlank(message = "视频地址不能为空")
    @Size(max = 255, message = "视频地址过长")
    @Pattern(regexp = "^/uploads/[A-Za-z0-9._-]+$", message = "视频地址格式不正确")
    private String url;
}
