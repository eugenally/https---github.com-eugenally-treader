package com.edu.bootstring.shipment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 출하 API 의 요청/응답 형태
 */
public final class ShipmentDtos {

    private ShipmentDtos() {
    }

    public record CreateItemRequest(
            @NotNull Long orderItemId,
            @NotNull @Positive BigDecimal qty,
            Integer cartonQty,
            BigDecimal netWeight,
            BigDecimal grossWeight,
            BigDecimal cbm
    ) {
    }

    public record CreateRequest(
            @NotNull Long orderId,
            String transportMode,
            LocalDate etd,
            LocalDate eta,
            String vesselName,
            String containerType,
            String containerNo,
            String portOfLoading,
            String portOfDischarge,
            BigDecimal totalCbm,
            Integer totalCarton,
            String remark,
            @NotEmpty List<CreateItemRequest> items
    ) {
    }

    /**
     * 출하 확정 요청. 재고가 실제로 차감되는 지점이다.
     * 해상은 B/L, 항공은 AWB 번호가 있어야 확정할 수 있다.
     */
    public record ConfirmRequest(
            String blNo,
            String awbNo,
            LocalDate shipDate
    ) {
    }

    public record ItemResponse(
            Long itemId,
            Long orderItemId,
            Long productId,
            String productCode,
            String productName,
            BigDecimal qty,
            Integer cartonQty,
            BigDecimal netWeight,
            BigDecimal grossWeight
    ) {
    }

    public record SummaryResponse(
            Long id,
            String shipmentNo,
            Long orderId,
            String orderNo,
            String customerName,
            String status,
            String transportMode,
            LocalDate etd,
            LocalDate shipDate,
            String blNo,
            BigDecimal totalGrossWeight,
            boolean deletable,
            boolean invoiced
    ) {
    }

    public record DetailResponse(
            Long id,
            String shipmentNo,
            Long orderId,
            String orderNo,
            String customerName,
            String status,
            String transportMode,
            LocalDate etd,
            LocalDate eta,
            LocalDate shipDate,
            String vesselName,
            String blNo,
            String awbNo,
            String containerType,
            String containerNo,
            String portOfLoading,
            String portOfDischarge,
            BigDecimal totalNetWeight,
            BigDecimal totalGrossWeight,
            BigDecimal totalCbm,
            Integer totalCarton,
            boolean deletable,
            boolean invoiced,
            List<ItemResponse> items
    ) {
    }
}
