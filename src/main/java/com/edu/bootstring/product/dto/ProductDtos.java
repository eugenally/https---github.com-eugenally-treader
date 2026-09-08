package com.edu.bootstring.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 제품·단가·재고 API 의 요청/응답 형태
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "제품 코드를 입력해 주세요.")
            @Pattern(regexp = "^[A-Za-z0-9\\-_]{2,50}$",
                    message = "제품 코드는 영문·숫자·하이픈 2~50자여야 합니다. 등록 후에는 바꿀 수 없습니다.")
            String productCode,

            @NotBlank(message = "영문 품명을 입력해 주세요.") @Size(max = 200) String nameEn,
            @Size(max = 200) String nameKo,
            @Size(max = 500) String spec,
            @Size(max = 20) String hsCode,
            @NotBlank(message = "단위를 입력해 주세요.") @Size(max = 10) String unit,
            @DecimalMin(value = "0", message = "MOQ 는 0 이상이어야 합니다.") BigDecimal moq,
            @Size(max = 50) String packageType,
            @Min(value = 1, message = "박스당 수량은 1 이상이어야 합니다.") Integer qtyPerCarton,
            @DecimalMin("0") BigDecimal netWeight,
            @DecimalMin("0") BigDecimal grossWeight,
            @DecimalMin("0") BigDecimal cbm,
            /** 등록과 동시에 초기 재고를 넣고 싶을 때 */
            @DecimalMin("0") BigDecimal initialStock
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String nameEn,
            @Size(max = 200) String nameKo,
            @Size(max = 500) String spec,
            @Size(max = 20) String hsCode,
            @NotBlank @Size(max = 10) String unit,
            @DecimalMin("0") BigDecimal moq,
            @Size(max = 50) String packageType,
            @Min(1) Integer qtyPerCarton,
            @DecimalMin("0") BigDecimal netWeight,
            @DecimalMin("0") BigDecimal grossWeight,
            @DecimalMin("0") BigDecimal cbm
    ) {
    }

    public record Summary(
            Long id,
            String productCode,
            String nameEn,
            String nameKo,
            String spec,
            String unit,
            BigDecimal moq,
            BigDecimal netWeight,
            BigDecimal grossWeight,
            BigDecimal cbm,
            String status,
            BigDecimal onHandQty,
            BigDecimal allocatedQty,
            BigDecimal availableQty,
            boolean deletable
    ) {
    }

    public record Detail(
            Long id,
            String productCode,
            String nameEn,
            String nameKo,
            String spec,
            String hsCode,
            String unit,
            BigDecimal moq,
            String packageType,
            Integer qtyPerCarton,
            BigDecimal netWeight,
            BigDecimal grossWeight,
            BigDecimal cbm,
            String status,
            BigDecimal onHandQty,
            BigDecimal allocatedQty,
            BigDecimal availableQty,
            boolean deletable,
            long priceCount,
            long usageCount
    ) {
    }

    // ------------------------------------------------------------------
    // 단가 이력
    // ------------------------------------------------------------------

    public record PriceCreateRequest(
            @NotNull(message = "제품을 선택해 주세요.") Long productId,
            /** 비우면 모든 거래처에 적용되는 표준 단가가 된다 */
            Long customerId,
            @Pattern(regexp = "^[A-Z]{3}$", message = "통화는 대문자 3자여야 합니다.") String currency,
            @NotNull(message = "단가를 입력해 주세요.")
            @DecimalMin(value = "0.0001", message = "단가는 0보다 커야 합니다.") BigDecimal unitPrice,
            @Size(max = 10) String incoterms,
            @DecimalMin("0") BigDecimal minQty,
            @NotNull(message = "적용 시작일을 입력해 주세요.") LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 500) String remark,
            /**
             * 기간이 겹칠 때 이전 단가를 자동으로 닫을지. 비우면 false 로 본다.
             *
             * <p>원시 {@code boolean} 이 아니라 {@code Boolean} 인 것이 의도다.
             * Boot 4 의 Jackson 3 은 {@code FAIL_ON_NULL_FOR_PRIMITIVES} 가 기본 켜져 있어
             * JSON 에서 이 필드가 빠지면 <b>본문 전체가 파싱 실패</b>한다. (Jackson 2 는 false 였다)
             */
            Boolean closePrevious
    ) {
        public boolean shouldClosePrevious() {
            return Boolean.TRUE.equals(closePrevious);
        }
    }

    public record PriceUpdateRequest(
            @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 10) String incoterms,
            @DecimalMin("0") BigDecimal minQty,
            LocalDate validTo,
            @Size(max = 500) String remark
    ) {
    }

    public record PriceResponse(
            Long id,
            Long productId,
            String productCode,
            String productName,
            Long customerId,
            String customerName,
            /** 거래처 전용 단가인지 표준 단가인지 */
            boolean standardPrice,
            String currency,
            BigDecimal unitPrice,
            String incoterms,
            BigDecimal minQty,
            LocalDate validFrom,
            LocalDate validTo,
            boolean effectiveNow,
            String remark
    ) {
    }

    // ------------------------------------------------------------------
    // 재고
    // ------------------------------------------------------------------

    /** 실사 조정. 차이 수량이 아니라 실제로 센 수량을 넣는다. */
    public record StockAdjustRequest(
            @NotNull(message = "실사 수량을 입력해 주세요.")
            @DecimalMin(value = "0", message = "재고는 음수가 될 수 없습니다.") BigDecimal countedQty,
            @Size(max = 500) String reason
    ) {
    }

    public record StockResponse(
            Long productId,
            String productCode,
            String productName,
            String unit,
            BigDecimal onHandQty,
            BigDecimal allocatedQty,
            BigDecimal availableQty,
            BigDecimal safetyQty,
            /** 가용 재고가 안전재고 아래로 내려갔는가 */
            boolean belowSafety
    ) {
    }
}
