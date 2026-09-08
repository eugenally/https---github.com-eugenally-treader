package com.edu.bootstring.customer;

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
 * 거래처 마스터
 */
@Entity
@Getter
@Table(name = "CUSTOMER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCustomer")
    @SequenceGenerator(name = "seqCustomer", sequenceName = "SEQ_CUSTOMER", allocationSize = 1)
    @Column(name = "CUSTOMER_ID")
    private Long id;

    @Column(name = "CUSTOMER_CODE", nullable = false, length = 8, updatable = false)
    private String customerCode;

    @Column(name = "NAME_EN", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "NAME_KO", length = 200)
    private String nameKo;

    @Column(name = "COUNTRY_CODE", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "DEFAULT_CURRENCY", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "DEFAULT_INCOTERMS", length = 10)
    private String defaultIncoterms;

    @Column(name = "PAYMENT_TERMS_CODE", nullable = false, length = 20)
    private String paymentTermsCode;

    @Column(name = "PAYMENT_DAYS")
    private Integer paymentDays;

    /** 선금 비율(%) — 견적 단계 PI 금액 산정에 쓰인다 (D5-09) */
    @Column(name = "ADVANCE_RATE", precision = 5, scale = 2)
    private BigDecimal advanceRate;

    /** 견적 유효기한 기본 일수 (D5-01) */
    @Column(name = "QUOTE_VALID_DAYS", nullable = false)
    private Integer quoteValidDays;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;
}
