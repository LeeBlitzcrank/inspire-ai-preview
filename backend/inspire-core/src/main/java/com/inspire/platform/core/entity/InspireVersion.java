package com.inspire.platform.core.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InspireVersion {
    private Long id;
    private Long inspireId;
    private Integer versionNumber;
    private String title;
    private String content;
    private String img;
    private String images;
    private String tag;
    private String changeSummary;
    private LocalDateTime createTime;
}
