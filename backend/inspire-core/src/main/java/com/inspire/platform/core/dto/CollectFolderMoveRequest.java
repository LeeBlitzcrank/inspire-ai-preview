package com.inspire.platform.core.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CollectFolderMoveRequest {
    @Positive(message = "收藏夹ID不正确")
    private Long folderId;
}
