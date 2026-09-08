package com.edu.bootstring.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 단가 이력 — (거래처 × 제품 × 기간) N:M 해소 테이블.
 * CUSTOMER_ID 가 NULL 이면 모든 거래처에 적용되는 표준 단가다.
 */
@Entity
@Getter
@Table(name = "PRICE_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPriceHistory")
    @SequenceGenerator(name = "seqPriceHistory", sequenceName = "SEQ_PRICE_HISTORY", allocationSize = 1)
    @Column(name = "PRICE_ID")
    private Long id;

    /** NULL = 표준 단가 */
    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "UNIT_PRICE", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "INCOTERMS", length = 10)
    private String incoterms;

    @Column(name = "MIN_QTY", precision = 15, scale = 3)
    private BigDecimal minQty;

    @Column(name = "VALID_FROM", nullable = false)
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;

    @Column(name = "REMARK", length = 500)
    private String remark;

    @Builder
    private PriceHistory(Long customerId, Long productId, String currency, BigDecimal unitPrice,
                         String incoterms, BigDecimal minQty, LocalDate validFrom, LocalDate validTo,
                         String remark) {
        this.customerId = customerId;
        this.productId = productId;
        this.currency = currency != null ? currency : "USD";
        this.unitPrice = unitPrice;
        this.incoterms = incoterms;
        this.minQty = minQty;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.remark = remark;
    }

    public void update(BigDecimal unitPrice, String currency, String incoterms,
                       BigDecimal minQty, LocalDate validTo, String remark) {
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.incoterms = incoterms;
        this.minQty = minQty;
        this.validTo = validTo;
        this.remark = remark;
    }

    /**
     * 새 단가가 들어올 때 기존 단가의 유효기간을 닫는다.
     *
     * <p>기간 중복은 DB 제약으로 막을 수 없어(범위 겹침은 UNIQUE 로 표현이 안 된다)
     * 서비스가 책임진다. 새 단가 시작일의 <b>하루 전</b>으로 닫아 하루도 겹치지 않게 한다.
     */
    public void closeBefore(LocalDate newValidFrom) {
        this.validTo = newValidFrom.minusDays(1);
    }

    public boolean isStandardPrice() {
        return customerId == null;
    }

    /** 기준일에 이 단가가 살아 있는가 */
    public boolean isEffectiveOn(LocalDate baseDate) {
        return !validFrom.isAfter(baseDate) && (validTo == null || !validTo.isBefore(baseDate));
    }

    /** 두 기간이 겹치는가 — 등록 전 검증에 쓴다 */
    public boolean overlaps(LocalDate from, LocalDate to) {
        LocalDate thisEnd = validTo != null ? validTo : LocalDate.of(9999, 12, 31);
        LocalDate otherEnd = to != null ? to : LocalDate.of(9999, 12, 31);
        return !validFrom.isAfter(otherEnd) && !from.isAfter(thisEnd);
    }
}
