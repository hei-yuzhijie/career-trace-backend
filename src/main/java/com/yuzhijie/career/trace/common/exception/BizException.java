package com.yuzhijie.career.trace.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常：body 内 code 为业务错误码，HTTP 状态码取对应的 4xx/5xx。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCodes.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    public static BizException badRequest(int code, String message) {
        return new BizException(code, HttpStatus.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ErrorCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    public static BizException notFound(int code, String message) {
        return new BizException(code, HttpStatus.NOT_FOUND, message);
    }

    public static BizException conflict(int code, String message) {
        return new BizException(code, HttpStatus.CONFLICT, message);
    }

    public static BizException internal(int code, String message) {
        return new BizException(code, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
