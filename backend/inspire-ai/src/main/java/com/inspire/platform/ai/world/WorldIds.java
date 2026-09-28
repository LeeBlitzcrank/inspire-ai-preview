package com.inspire.platform.ai.world;

import java.util.concurrent.atomic.AtomicLong;

final class WorldIds {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private WorldIds() {
    }

    static long next() {
        long timestamp = System.currentTimeMillis();
        long sequence = SEQUENCE.updateAndGet(value -> (value + 1) & 0xFFF);
        return timestamp * 4096 + sequence;
    }
}
