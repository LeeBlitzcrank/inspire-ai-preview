package com.inspire.platform.auth.service;

public interface SmsSender {
    void send(String phone, String code, String purpose);
}
