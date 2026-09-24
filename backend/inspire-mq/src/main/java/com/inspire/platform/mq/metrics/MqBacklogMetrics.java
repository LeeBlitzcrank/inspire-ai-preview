package com.inspire.platform.mq.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class MqBacklogMetrics {

    private record ConsumerTarget(String group, String topic) {
    }

    private static final List<ConsumerTarget> TARGETS = List.of(
            new ConsumerTarget("consumer_register", "topic_user_register"),
            new ConsumerTarget("consumer_behavior", "topic_user_behavior"),
            new ConsumerTarget("consumer_publish", "topic_inspire_publish")
    );

    private final MeterRegistry meterRegistry;
    private final String nameServer;
    private final Map<String, AtomicLong> lagValues = new ConcurrentHashMap<>();
    private DefaultMQAdminExt admin;

    public MqBacklogMetrics(MeterRegistry meterRegistry,
                            @Value("${rocketmq.name-server:}") String nameServer) {
        this.meterRegistry = meterRegistry;
        this.nameServer = nameServer;
        for (ConsumerTarget target : TARGETS) {
            AtomicLong holder = new AtomicLong(0);
            lagValues.put(target.group() + ":" + target.topic(), holder);
            Gauge.builder("inspire.mq.consumer.lag", holder, AtomicLong::get)
                    .tag("group", target.group())
                    .tag("topic", target.topic())
                    .register(meterRegistry);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void collect() {
        if (nameServer == null || nameServer.isBlank()) return;
        try {
            ensureAdmin();
            for (ConsumerTarget target : TARGETS) {
                ConsumeStats stats = admin.examineConsumeStats(target.group(), target.topic());
                long lag = Math.max(0L, stats.computeTotalDiff());
                lagValues.get(target.group() + ":" + target.topic()).set(lag);
            }
        } catch (Exception e) {
            log.debug("MQ积压指标采集失败: {}", e.getMessage());
        }
    }

    private synchronized void ensureAdmin() throws Exception {
        if (admin != null) return;
        DefaultMQAdminExt created = new DefaultMQAdminExt("inspire_metrics_admin");
        created.setNamesrvAddr(nameServer);
        created.start();
        admin = created;
    }

    @PreDestroy
    public void destroy() {
        if (admin != null) admin.shutdown();
    }
}
