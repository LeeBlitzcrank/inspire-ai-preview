package com.inspire.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPushRequest {
    @NotBlank(message = "推送标题不能为空")
    @Size(max = 120, message = "推送标题不能超过120个字符")
    private String title;

    @NotBlank(message = "推送内容不能为空")
    @Size(max = 200, message = "推送内容不能超过200个字符")
    private String content;

    @Size(max = 32, message = "推送城市不能超过32个字符")
    private String city;
}
