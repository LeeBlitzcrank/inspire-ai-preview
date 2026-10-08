package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.inspire.platform.common.validation.ValidationConstants.MEDIA_URL_MAX;

@Data
public class UploadFromUrlRequest {
    @NotBlank(message = "图片URL不能为空")
    @Size(max = MEDIA_URL_MAX, message = "图片URL过长")
    private String url;
}
