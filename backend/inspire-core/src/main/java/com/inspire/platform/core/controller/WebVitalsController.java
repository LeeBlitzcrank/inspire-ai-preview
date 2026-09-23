package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.service.impl.InspireServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/inspire/public")
@RequiredArgsConstructor
public class WebVitalsController {

    private static final Set<String> METRICS = Set.of("FCP", "LCP", "CLS", "INP", "TTFB");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/vitals")
    public Result<Void> record(@RequestBody Map<String, Object> body,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            String name = text(body.get("name"), 16);
            if (!METRICS.contains(name)) {
                return Result.success();
            }
            double value = number(body.get("value"));
            if (!Double.isFinite(value) || value < 0 || value > 300_000) {
                return Result.success();
            }
            jdbcTemplate.update(
                    "INSERT INTO web_vital_metric(id,metric_name,metric_value,metric_rating,metric_delta,"
                            + "navigation_type,page_path,device_type,browser,app_version,user_id,create_time) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                    InspireServiceImpl.nextId(),
                    name,
                    value,
                    text(body.get("rating"), 16),
                    number(body.get("delta")),
                    text(body.get("navigationType"), 32),
                    text(body.get("path"), 255),
                    text(body.get("device"), 32),
                    text(body.get("browser"), 64),
                    text(body.get("appVersion"), 64),
                    userId,
                    LocalDateTime.now(ZONE));
        } catch (Exception e) {
            log.debug("web-vitals 记录失败: {}", e.getMessage());
        }
        return Result.success();
    }

    private String text(Object value, int maxLength) {
        String text = value == null ? "" : String.valueOf(value);
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0d;
        }
    }
}
