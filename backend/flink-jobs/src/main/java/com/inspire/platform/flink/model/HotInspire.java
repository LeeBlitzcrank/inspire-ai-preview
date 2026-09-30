/**
 * 文件：backend/flink-jobs/src/main/java/com/inspire/platform/flink/model/HotInspire.java
 * 所属模块：实时计算模块，负责热点聚合和用户行为计算
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.flink.model;
public class HotInspire {
    public String city; public String tag; public String timeSlot;
    public long count; public long windowEnd;

    @Override public String toString() {
        return String.format("{city=%s, tag=%s, time=%s, count=%d}", city, tag, timeSlot, count);
    }
}
