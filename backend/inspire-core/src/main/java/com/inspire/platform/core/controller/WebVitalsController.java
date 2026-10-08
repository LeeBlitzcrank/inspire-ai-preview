/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/controller/WebVitalsController.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：HTTP 接口控制器，负责参数接收、权限上下文和响应返回
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.controller;

import com.inspire.platform.common.result.Result;
import com.inspire.platform.core.dto.WebVitalRequest;
import com.inspire.platform.core.service.impl.InspireServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@RestController
@RequestMapping("/inspire/public")
@RequiredArgsConstructor
public class WebVitalsController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/vitals")
    public Result<Void> record(@Valid @RequestBody WebVitalRequest request,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO web_vital_metric(id,metric_name,metric_value,metric_rating,metric_delta,"
                            + "navigation_type,page_path,device_type,browser,app_version,user_id,create_time) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                    InspireServiceImpl.nextId(),
                    request.getName(),
                    request.getValue(),
                    text(request.getRating(), 16),
                    request.getDelta(),
                    text(request.getNavigationType(), 32),
                    text(request.getPath(), 255),
                    text(request.getDevice(), 32),
                    text(request.getBrowser(), 64),
                    text(request.getAppVersion(), 64),
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

}
