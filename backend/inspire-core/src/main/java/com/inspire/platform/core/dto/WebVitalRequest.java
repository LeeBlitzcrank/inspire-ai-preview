package com.inspire.platform.core.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class WebVitalRequest {
    @NotBlank(message = "性能指标名称不能为空")
    @Pattern(regexp = "FCP|LCP|CLS|INP|TTFB", message = "性能指标名称不正确")
    private String name;

    @DecimalMin(value = "0", message = "性能指标数值不正确")
    @DecimalMax(value = "300000", message = "性能指标数值不正确")
    private double value;

    @Size(max = 16, message = "性能评级过长")
    private String rating;

    private double delta;

    @Size(max = 32, message = "导航类型过长")
    private String navigationType;

    @Size(max = 255, message = "页面路径过长")
    private String path;

    @Size(max = 32, message = "设备类型过长")
    private String device;

    @Size(max = 64, message = "浏览器信息过长")
    private String browser;

    @Size(max = 64, message = "应用版本过长")
    private String appVersion;
}
