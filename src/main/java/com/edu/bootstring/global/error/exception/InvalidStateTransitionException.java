package com.edu.bootstring.global.error.exception;

import com.edu.bootstring.global.error.ErrorCode;

/**
 * 상태 전이 규칙 위반 시 발생하는 예외
 */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String message) {
        super(message, ErrorCode.INVALID_STATE_TRANSITION);
    }

    public InvalidStateTransitionException() {
        super(ErrorCode.INVALID_STATE_TRANSITION);
    }
}
