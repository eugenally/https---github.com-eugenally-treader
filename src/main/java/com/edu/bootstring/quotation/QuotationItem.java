package com.edu.bootstring.quotation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 견적 품목 라인
 */
@Entity
@Getter
@Table(name = "QUOTATION_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqQuotationItem")
    @SequenceGenerator(name = "seqQuotationItem", sequenceName = "SEQ_QUOTATION_ITEM", allocationSize = 1)
    @Column(name = "ITEM_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QUOTATION_ID", nullable = false)
    private Quotation quotation;

    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal qty;

    @Column(name = "UNIT_PRICE", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "REMARK", length = 500)
    private String remark;

    @Builder
    private QuotationItem(Integer lineNo, Long productId, BigDecimal qty,
                          BigDecimal unitPrice, String remark) {
        this.lineNo = lineNo;
        this.productId = productId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.amount = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        this.remark = remark;
    }

    void assignTo(Quotation quotation) {
        this.quotation = quotation;
    }
}
