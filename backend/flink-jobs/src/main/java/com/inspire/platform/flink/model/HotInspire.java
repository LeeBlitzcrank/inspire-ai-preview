package com.inspire.platform.flink.model;
public class HotInspire {
    public String city; public String tag; public String timeSlot;
    public long count; public long windowEnd;

    @Override public String toString() {
        return String.format("{city=%s, tag=%s, time=%s, count=%d}", city, tag, timeSlot, count);
    }
}
