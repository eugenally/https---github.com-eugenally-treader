package com.edu.bootstring.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 인보이스 API 의 요청/응답 형태
 */
public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    /** 출하 1건에 대한 CI 발행 요청 (D5-10: 출하 1건 = CI 1장) */
    public record IssueCiRequest(
            @NotNull Long shipmentId,
            LocalDate issueDate
    ) {
    }

    /** 견적 단계 선금 PI 발행 요청 (D5-09) */
    public record IssuePiRequest(
            @NotNull Long quotationId,
            LocalDate issueDate
    ) {
    }

    public record PaymentRequest(
            @NotNull @Positive BigDecimal amount,
            LocalDate paidDate,
            String method,
            String bankRef
    ) {
    }

    public record PaymentResponse(
            Long id,
            LocalDate paidDate,
            BigDecimal amount,
            String method,
            String bankRef
    ) {
    }

    public record Response(
            Long id,
            String invoiceNo,
            String invoiceType,
            Long quotationId,
            Long orderId,
            String orderNo,
            Long shipmentId,
            String shipmentNo,
            Long customerId,
            String customerName,
            String status,
            String currency,
            BigDecimal amount,
            BigDecimal paidAmount,
            BigDecimal balance,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate rateBaseDate,
            BigDecimal exchangeRate,
            BigDecimal krwAmount,
            List<PaymentResponse> payments
    ) {
    }
}
