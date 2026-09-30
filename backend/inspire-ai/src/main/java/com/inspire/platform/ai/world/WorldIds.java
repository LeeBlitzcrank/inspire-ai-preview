/**
 * 文件：backend/inspire-ai/src/main/java/com/inspire/platform/ai/world/WorldIds.java
 * 所属模块：AI 服务模块，负责创作生成、AI 探索和世界种子
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
