/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/dto/LoginRequest.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data @Schema(description = "管理员登录请求")
public class LoginRequest {
    @Schema(description = "管理员账号", example = "admin")
    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 50, message = "管理员账号过长")
    private String username;
    @Schema(description = "密码", example = "112233")
    @NotBlank(message = "管理员密码不能为空")
    @Size(max = 128, message = "管理员密码过长")
    private String password;
    @Schema(description = "TOTP 动态验证码")
    @Pattern(regexp = "^$|\\d{6}", message = "动态验证码必须为6位数字")
    private String mfaCode;
}
