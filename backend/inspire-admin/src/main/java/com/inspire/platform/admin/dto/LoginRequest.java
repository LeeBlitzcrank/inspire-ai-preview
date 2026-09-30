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
import lombok.Data;
@Data @Schema(description = "管理员登录请求")
public class LoginRequest {
    @Schema(description = "管理员账号", example = "admin")
    @NotBlank private String username;
    @Schema(description = "密码", example = "112233")
    @NotBlank private String password;
    @Schema(description = "TOTP 动态验证码")
    private String mfaCode;
}
