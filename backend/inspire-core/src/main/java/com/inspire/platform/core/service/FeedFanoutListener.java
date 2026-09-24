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
