package com.edu.bootstring.invoice;

/**
 * 인보이스 상태. 입금 누계로 자동 판정되므로 전이표 대신 판정 규칙을 둔다.
 */
public enum InvoiceStatus {

    ISSUED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}
