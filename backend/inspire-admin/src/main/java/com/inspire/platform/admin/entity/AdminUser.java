package com.inspire.platform.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("admin_user")
@Schema(description = "管理员")
public class AdminUser {
    @Schema(description = "管理员ID")
    @TableId(type = IdType.AUTO)
    private Integer id;
    @Schema(description = "管理员账号", example = "admin")
    private String username;
    @Schema(hidden = true)
    private String password;
    @Schema(description = "管理员昵称", example = "超级管理员")
    private String nickname;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
