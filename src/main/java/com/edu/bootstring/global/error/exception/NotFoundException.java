package com.edu.bootstring.global.error.exception;

import com.edu.bootstring.global.error.ErrorCode;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message, ErrorCode.ENTITY_NOT_FOUND);
    }

    public NotFoundException() {
        super(ErrorCode.ENTITY_NOT_FOUND);
    }
}
