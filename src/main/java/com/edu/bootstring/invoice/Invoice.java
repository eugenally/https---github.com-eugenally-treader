package com.edu.bootstring.invoice;

import com.edu.bootstring.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 인보이스. PI 와 CI 두 종류를 한 테이블에 담는다.
 *
 * <p>PI 는 견적 단계에서도 발행할 수 있어 ORDER_ID 가 비어 있을 수 있다 (D5-09).
 * CI 는 출하 1건당 정확히 1장이다 (D5-10).
 *
 * <p>환율 기산일이 서로 다르다 (D5-14): PI 는 발행일, CI 는 선적일(ETD).
 * 어느 날짜를 썼는지 {@code rateBaseDate} 에 남긴다.
 */
@Entity
@Getter
@Table(name = "INVOICE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVOICE_ID")
    private Long id;

    @Column(name = "INVOICE_NO", nullable = false, length = 40, updatable = false)
    private String invoiceNo;

    /** PI / CI */
    @Column(name = "INVOICE_TYPE", nullable = false, length = 10)
    private String invoiceType;

    @Column(name = "QUOTATION_ID")
    private Long quotationId;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "SHIPMENT_ID")
    private Long shipmentId;

    @Column(name = "ISSUE_DATE", nullable = false)
    private LocalDate issueDate;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** 환율을 어느 날짜 기준으로 고정했는지 */
    @Column(name = "RATE_BASE_DATE")
    private LocalDate rateBaseDate;

    @Column(name = "EXCHANGE_RATE", precision = 15, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "KRW_AMOUNT", precision = 18, scale = 2)
    private BigDecimal krwAmount;

    @Column(name = "PAID_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "PAYMENT_TERMS_CODE", length = 20)
    private String paymentTermsCode;

    @Builder
    private Invoice(String invoiceNo, String invoiceType, Long quotationId, Long orderId,
                    Long shipmentId, LocalDate issueDate, LocalDate dueDate, String currency,
                    BigDecimal amount, LocalDate rateBaseDate, BigDecimal exchangeRate,
                    String paymentTermsCode) {
        this.invoiceNo = invoiceNo;
        this.invoiceType = invoiceType;
        this.quotationId = quotationId;
        this.orderId = orderId;
        this.shipmentId = shipmentId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.currency = currency;
        this.amount = amount;
        this.rateBaseDate = rateBaseDate;
        this.exchangeRate = exchangeRate;
        this.krwAmount = exchangeRate == null
                ? null
                : amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        this.paymentTermsCode = paymentTermsCode;
        this.paidAmount = BigDecimal.ZERO;
        this.status = InvoiceStatus.ISSUED;
    }

    public BigDecimal getBalance() {
        return amount.subtract(paidAmount);
    }

    /**
     * 입금 누계로 상태를 다시 판정한다.
     * 취소된 인보이스는 건드리지 않는다.
     */
    public void applyPayment(BigDecimal paidTotal, LocalDate today) {
        if (status == InvoiceStatus.CANCELLED) {
            return;
        }
        this.paidAmount = paidTotal;

        if (paidTotal.compareTo(amount) >= 0) {
            this.status = InvoiceStatus.PAID;
        } else if (paidTotal.signum() > 0) {
            this.status = dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : InvoiceStatus.PARTIALLY_PAID;
        } else {
            this.status = dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : InvoiceStatus.ISSUED;
        }
    }
}
