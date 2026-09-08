package com.edu.bootstring.inventory;

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
import java.time.LocalDateTime;

/**
 * 재고 변동 이력. 되돌리기와 감사 추적의 근거가 된다.
 */
@Entity
@Getter
@Table(name = "INVENTORY_TXN")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqInventoryTxn")
    @SequenceGenerator(name = "seqInventoryTxn", sequenceName = "SEQ_INVENTORY_TXN", allocationSize = 1)
    @Column(name = "TXN_ID")
    private Long id;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    /** IN / OUT / ALLOC / RELEASE / ADJUST */
    @Column(name = "TXN_TYPE", nullable = false, length = 20)
    private String txnType;

    /** 부호 있는 변동량 (출고는 음수) */
    @Column(name = "QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal qty;

    @Column(name = "BEFORE_QTY", precision = 15, scale = 3)
    private BigDecimal beforeQty;

    @Column(name = "AFTER_QTY", precision = 15, scale = 3)
    private BigDecimal afterQty;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;

    @Column(name = "REF_ID")
    private Long refId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private InventoryTxn(Long productId, String txnType, BigDecimal qty,
                         BigDecimal beforeQty, BigDecimal afterQty,
                         String refType, Long refId) {
        this.productId = productId;
        this.txnType = txnType;
        this.qty = qty;
        this.beforeQty = beforeQty;
        this.afterQty = afterQty;
        this.refType = refType;
        this.refId = refId;
        this.createdAt = LocalDateTime.now();
    }
}
