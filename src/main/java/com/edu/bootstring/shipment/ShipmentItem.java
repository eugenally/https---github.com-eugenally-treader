package com.edu.bootstring.shipment;

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

/**
 * 출하 품목. 어느 수주 라인을 얼마나 채웠는지를 가리킨다.
 */
@Entity
@Getter
@Table(name = "SHIPMENT_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHIPMENT_ID", nullable = false)
    private Shipment shipment;

    @Column(name = "ORDER_ITEM_ID", nullable = false)
    private Long orderItemId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal qty;

    @Column(name = "CARTON_QTY")
    private Integer cartonQty;

    @Column(name = "NET_WEIGHT", precision = 15, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "GROSS_WEIGHT", precision = 15, scale = 3)
    private BigDecimal grossWeight;

    @Column(name = "CBM", precision = 15, scale = 4)
    private BigDecimal cbm;

    @Builder
    private ShipmentItem(Long orderItemId, Long productId, BigDecimal qty, Integer cartonQty,
                         BigDecimal netWeight, BigDecimal grossWeight, BigDecimal cbm) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.qty = qty;
        this.cartonQty = cartonQty;
        this.netWeight = netWeight;
        this.grossWeight = grossWeight;
        this.cbm = cbm;
    }

    void assignTo(Shipment shipment) {
        this.shipment = shipment;
    }
}
