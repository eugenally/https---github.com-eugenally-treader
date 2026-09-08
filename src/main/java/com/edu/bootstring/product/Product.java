package com.edu.bootstring.product;

import com.edu.bootstring.global.common.BaseTimeEntity;
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

/**
 * 제품 마스터
 */
@Entity
@Getter
@Table(name = "PRODUCT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProduct")
    @SequenceGenerator(name = "seqProduct", sequenceName = "SEQ_PRODUCT", allocationSize = 1)
    @Column(name = "PRODUCT_ID")
    private Long id;

    @Column(name = "PRODUCT_CODE", nullable = false, length = 50, updatable = false)
    private String productCode;

    @Column(name = "NAME_EN", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "NAME_KO", length = 200)
    private String nameKo;

    @Column(name = "SPEC", length = 500)
    private String spec;

    @Column(name = "HS_CODE", length = 20)
    private String hsCode;

    @Column(name = "UNIT", nullable = false, length = 10)
    private String unit;

    /** 최소 주문 수량 — 미만이어도 막지 않고 경고만 한다 */
    @Column(name = "MOQ", precision = 15, scale = 3)
    private BigDecimal moq;

    @Column(name = "QTY_PER_CTN")
    private Integer qtyPerCarton;

    @Column(name = "NET_WEIGHT", precision = 15, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "GROSS_WEIGHT", precision = 15, scale = 3)
    private BigDecimal grossWeight;

    @Column(name = "CBM", precision = 15, scale = 4)
    private BigDecimal cbm;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;
}
