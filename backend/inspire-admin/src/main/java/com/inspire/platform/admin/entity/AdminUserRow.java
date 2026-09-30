/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/entity/AdminUserRow.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：接口或查询数据传输模型，定义字段结构
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
