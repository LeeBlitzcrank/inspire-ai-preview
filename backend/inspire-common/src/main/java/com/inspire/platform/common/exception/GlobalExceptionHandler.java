package com.inspire.platform.common.exception;

import com.inspire.platform.common.model.ErrorCode;
import com.inspire.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获业务异常、参数校验异常、系统异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 需要返回 401 的文档错误码集合 */
    private static final Set<Integer> UNAUTHORIZED_CODES = Set.of(
            ErrorCode.TOKEN_MISSING.getCode(),
            ErrorCode.TOKEN_INVALIDATED.getCode(),
            ErrorCode.TOKEN_SIGNATURE_INVALID.getCode(),
            ErrorCode.TOKEN_EXPIRED.getCode(),
            ErrorCode.REFRESH_TOKEN_INVALID.getCode()
    );

    /** 需要返回 403 的文档错误码集合 */
    private static final Set<Integer> FORBIDDEN_CODES = Set.of(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN_ROLE.getCode()
    );

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        int code = e.getCode();

        // 根据文档错误码设置响应状态
        if (UNAUTHORIZED_CODES.contains(code)) {
            // 401xxx 系列：认证失败
            log.warn("认证失败: code={}, msg={}", code, e.getMessage());
            return Result.error(code, e.getMessage());
        }
        if (FORBIDDEN_CODES.contains(code)) {
            // 403xxx 系列：权限不足
            log.warn("权限不足: code={}, msg={}", code, e.getMessage());
            return Result.error(code, e.getMessage());
        }

        log.warn("业务异常: code={}, msg={}", code, e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }



    @ExceptionHandler({MissingServletRequestParameterException.class, TypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return Result.error(400, "请求参数错误: " + e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("服务异常，请稍后重试");
    }

    /**
     * 捕获 UserContext.requireUserId() 抛出的未登录异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        log.warn("未登录访问: {}", e.getMessage());
        return Result.error(ErrorCode.TOKEN_MISSING.getCode(), "未登录，请先登录");
    }
}
