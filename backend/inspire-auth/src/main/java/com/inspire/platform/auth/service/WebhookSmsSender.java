package com.inspire.platform.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookSmsSender implements SmsSender {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    @Value("${inspire.sms.webhook-url:}")
    private String webhookUrl;

    @Value("${inspire.sms.webhook-token:}")
    private String webhookToken;

    @Value("${inspire.sms.log-code:false}")
    private boolean logCode;

    @Override
    public void send(String phone, String code, String purpose) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            if (!logCode) {
                throw new BusinessException(503, "短信服务未配置，请联系管理员");
            }
            log.warn("[SMS-DEV] 未配置短信Webhook，手机号={}，用途={}，验证码={}",
                    maskPhone(phone), purpose, code);
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "code", code,
                    "purpose", purpose
            ));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (webhookToken != null && !webhookToken.isBlank()) {
                builder.header("Authorization", "Bearer " + webhookToken);
            }
            HttpResponse<String> response = HTTP_CLIENT.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("短信Webhook返回异常: status={}, body={}",
                        response.statusCode(), abbreviate(response.body()));
                throw new BusinessException(502, "验证码发送失败，请稍后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("短信Webhook调用失败: phone={}, error={}", maskPhone(phone), e.getMessage());
            throw new BusinessException(502, "验证码发送失败，请稍后重试");
        }
    }

    private String maskPhone(String phone) {
        return phone == null || phone.length() < 7
                ? phone
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }
}
