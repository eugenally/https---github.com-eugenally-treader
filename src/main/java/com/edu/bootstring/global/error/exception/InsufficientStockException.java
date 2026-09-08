package com.edu.bootstring.global.error.exception;

import com.edu.bootstring.global.error.ErrorCode;

/**
 * 출하 확정 시 실물 재고 부족 예외
 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String message) {
        super(message, ErrorCode.INSUFFICIENT_STOCK);
    }

    public InsufficientStockException() {
        super(ErrorCode.INSUFFICIENT_STOCK);
    }
}
