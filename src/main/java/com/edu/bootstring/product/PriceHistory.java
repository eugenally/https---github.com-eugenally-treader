package com.edu.bootstring.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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

    @Column(name = "VALID_FROM", nullable = false)
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;
}
