package com.edu.bootstring.order;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 수주 품목 라인. 한 라인을 여러 번 나눠 출하할 수 있어서 {@code shippedQty} 를 누적한다.
 */
@Entity
@Getter
@Table(name = "SALES_ORDER_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private SalesOrder order;

    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "ORDERED_QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal orderedQty;

    @Column(name = "SHIPPED_QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal shippedQty;

    @Column(name = "UNIT_PRICE", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "REMARK", length = 500)
    private String remark;

    @Builder
    private SalesOrderItem(Integer lineNo, Long productId, BigDecimal orderedQty,
                           BigDecimal unitPrice, String remark) {
        this.lineNo = lineNo;
        this.productId = productId;
        this.orderedQty = orderedQty;
        this.shippedQty = BigDecimal.ZERO;
        this.unitPrice = unitPrice;
        this.amount = orderedQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        this.remark = remark;
    }

    void assignTo(SalesOrder order) {
        this.order = order;
    }

    /** 아직 출하되지 않은 수량 */
    public BigDecimal getPendingQty() {
        return orderedQty.subtract(shippedQty);
    }

    public boolean isFullyShipped() {
        return shippedQty.compareTo(orderedQty) >= 0;
    }

    /** 출하 확정 시 누적. 잔량을 넘기면 거절한다. */
    public void addShippedQty(BigDecimal qty) {
        if (qty.compareTo(getPendingQty()) > 0) {
            throw new BusinessException(
                    "출하 수량이 잔량을 초과합니다. 잔량 %s, 요청 %s".formatted(getPendingQty(), qty),
                    ErrorCode.INVALID_INPUT_VALUE);
        }
        this.shippedQty = this.shippedQty.add(qty);
    }

    /** 출하 취소 시 되돌림 */
    public void subtractShippedQty(BigDecimal qty) {
        this.shippedQty = this.shippedQty.subtract(qty).max(BigDecimal.ZERO);
    }
}
