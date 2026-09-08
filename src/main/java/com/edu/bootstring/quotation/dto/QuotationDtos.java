package com.edu.bootstring.quotation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 견적 API 의 요청/응답 형태
 */
public final class QuotationDtos {

    private QuotationDtos() {
    }

    /** 새 견적 생성 요청 */
    public record CreateRequest(
            @NotNull Long customerId,
            LocalDate quoteDate,
            LocalDate validUntil,
            String currency,
            String incoterms,
            String portOfLoading,
            String portOfDischarge,
            String remark
    ) {
    }

    /** 품목 추가 요청 */
    public record AddItemRequest(
            @NotNull Long productId,
            @NotNull @Positive BigDecimal qty,
            /** 비우면 단가 이력에서 자동 제안된 값을 쓴다 */
            BigDecimal unitPrice,
            String remark
    ) {
    }

    public record ItemResponse(
            Long itemId,
            Integer lineNo,
            Long productId,
            String productCode,
            String productName,
            String unit,
            BigDecimal qty,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }

    public record SummaryResponse(
            Long id,
            String quoteNo,
            Integer revNo,
            Long customerId,
            String customerName,
            String status,
            String currency,
            BigDecimal totalAmount,
            LocalDate quoteDate,
            LocalDate validUntil,
            boolean converted
    ) {
    }

    public record DetailResponse(
            Long id,
            String quoteNo,
            Integer revNo,
            Long customerId,
            String customerName,
            String status,
            String currency,
            String incoterms,
            String portOfLoading,
            String portOfDischarge,
            LocalDate quoteDate,
            LocalDate validUntil,
            BigDecimal totalAmount,
            BigDecimal exchangeRate,
            BigDecimal krwAmount,
            String remark,
            boolean editable,
            boolean converted,
            Long convertedOrderId,
            /** 거래처 선금 비율(%). 0 이면 전액 후불이라 PI 를 낼 수 없다. */
            BigDecimal advanceRate,
            /** 선금 PI 를 낼 수 있는 상태인지 — 화면 버튼 노출 조건 */
            boolean piIssuable,
            Long piInvoiceId,
            String piInvoiceNo,
            List<ItemResponse> items
    ) {
    }

    /** 단가 자동 제안 응답 */
    public record PriceSuggestion(
            Long productId,
            BigDecimal unitPrice,
            String currency,
            /** 거래처 전용 단가인지, 표준 단가인지 */
            String source
    ) {
    }
}
