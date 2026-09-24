package com.inspire.platform.core.service;

import java.time.LocalDateTime;

public record FeedFanoutEvent(Long inspireId, Long authorId, LocalDateTime createTime) {
}
