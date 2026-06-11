package com.interviewai.common;

import lombok.Getter;

/**
 * 业务异常。在Service层抛出，由GlobalExceptionHandler统一处理。
 * 这比到处写try-catch + return Result.fail()更干净。
 */
@Getter
public class BizException extends RuntimeException {
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
