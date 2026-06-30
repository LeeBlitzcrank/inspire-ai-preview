package com.inspire.platform.admin.mapper;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("inspire_main")
public class InspireMainRow {
    @TableId private Long id;
    private String title; private String img; private String tag; private Long userId;
    private Integer status; private Long viewCount; private Integer likeCount;
    private Integer collectCount; private Integer heat; private String publishCity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted; private String extJson;
}
