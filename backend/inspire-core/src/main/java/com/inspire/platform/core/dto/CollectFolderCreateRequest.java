package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CollectFolderCreateRequest {
    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 20, message = "收藏夹名称不能超过20个字符")
    private String name;

    @Size(max = 20, message = "收藏夹图标过长")
    private String icon;
}
