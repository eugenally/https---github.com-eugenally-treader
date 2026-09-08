package com.edu.bootstring.global.error.exception;

import com.edu.bootstring.global.error.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직 예외의 공통 최상위 예외
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
