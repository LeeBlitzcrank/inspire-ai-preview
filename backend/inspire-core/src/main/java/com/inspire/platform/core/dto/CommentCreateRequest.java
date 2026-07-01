package com.inspire.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    private Long inspireId;
    @NotBlank
    private String content;
    private String username;
    private String avatar;
    private Long parentId;
    private Long replyUserId;
    private String replyUsername;
}
