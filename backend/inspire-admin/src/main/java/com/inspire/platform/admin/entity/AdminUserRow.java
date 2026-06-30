package com.inspire.platform.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("user") @Schema(description = "用户信息（管理员视角）")
public class AdminUserRow {
    @Schema(description = "用户ID", example = "197197582563282945")
    @JsonSerialize(using = ToStringSerializer.class) @TableId private Long id;
    @Schema(description = "用户名") private String username;
    @Schema(description = "邮箱") private String email;
    @Schema(description = "昵称") private String nickname;
    @Schema(description = "头像") private String avatar;
    @Schema(description = "城市") private String city;
    @Schema(description = "注册时间") private LocalDateTime createTime;
    @Schema(description = "0正常 1已删除") private Integer deleted;
}
