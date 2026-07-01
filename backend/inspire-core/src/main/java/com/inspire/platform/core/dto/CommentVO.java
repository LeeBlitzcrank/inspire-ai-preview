package com.inspire.platform.core.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

@Data
public class CommentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long inspireId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String username;
    private String avatar;
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyUserId;
    private String replyUsername;
    private LocalDateTime createTime;
    private List<CommentVO> children;
}
