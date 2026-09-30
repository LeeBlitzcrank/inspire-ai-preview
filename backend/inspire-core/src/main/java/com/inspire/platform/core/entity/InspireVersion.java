/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/entity/InspireVersion.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
