/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/service/FeedFanoutListener.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedFanoutListener {

    private final FeedService feedService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(FeedFanoutEvent event) {
        try {
            feedService.fanout(event.inspireId(), event.authorId(), event.createTime());
        } catch (Exception e) {
            log.warn("关注流异步扇出失败: inspireId={}", event.inspireId(), e);
        }
    }
}
