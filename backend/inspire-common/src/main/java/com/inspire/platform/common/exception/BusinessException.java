package com.inspire.platform.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * 统一由全局异常处理器捕获，返回 Result 格式
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
