/**
 * 文件：backend/inspire-admin/src/main/java/com/inspire/platform/admin/entity/SupportTicketAttachmentAdmin.java
 * 所属模块：后台管理模块，负责管理员鉴权、内容审核和运营配置
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_ticket_attachment")
public class SupportTicketAttachmentAdmin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String fileUrl;
    private String thumbUrl;
    private String fileType;
    private String originalName;
    private String duration;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
