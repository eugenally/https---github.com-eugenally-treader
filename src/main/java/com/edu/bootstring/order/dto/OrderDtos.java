package com.edu.bootstring.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 수주 API 의 요청/응답 형태
 */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** 견적 → 수주 전환 요청 */
    public record ConvertRequest(
            LocalDate requiredDate,
            String poNo,
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
            BigDecimal orderedQty,
            BigDecimal shippedQty,
            BigDecimal pendingQty,
            BigDecimal unitPrice,
            BigDecimal amount,
            /** 화면 진행률 표시용 (0~100) */
            int progressPercent
    ) {
    }

    /** 수주 상세에 함께 실려 나가는 출하 이력 */
    public record ShipmentSummary(
            Long shipmentId,
            String shipmentNo,
            String status,
            String transportMode,
            LocalDate etd,
            LocalDate shipDate,
            String containerType,
            String blNo,
            BigDecimal totalGrossWeight
    ) {
    }

    public record SummaryResponse(
            Long id,
            String orderNo,
            Long customerId,
            String customerName,
            String status,
            String currency,
            BigDecimal totalAmount,
            LocalDate orderDate,
            LocalDate requiredDate
    ) {
    }

    public record DetailResponse(
            Long id,
            String orderNo,
            Long quotationId,
            String quoteNo,
            Long customerId,
            String customerName,
            String status,
            String currency,
            String incoterms,
            String portOfLoading,
            String portOfDischarge,
            LocalDate orderDate,
            LocalDate requiredDate,
            BigDecimal totalAmount,
            String poNo,
            String remark,
            boolean cancellable,
            List<ItemResponse> items,
            List<ShipmentSummary> shipments
    ) {
    }

    /** 출하 등록 화면이 쓰는 잔량 목록 */
    public record PendingItemResponse(
            Long orderItemId,
            Integer lineNo,
            Long productId,
            String productCode,
            String productName,
            String unit,
            BigDecimal orderedQty,
            BigDecimal shippedQty,
            BigDecimal pendingQty,
            BigDecimal netWeightPerUnit,
            BigDecimal grossWeightPerUnit,
            BigDecimal onHandQty
    ) {
    }
}
