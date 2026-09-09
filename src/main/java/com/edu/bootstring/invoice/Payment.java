package com.edu.bootstring.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 입금 기록. 인보이스 상태는 이 행들의 합계로 판정된다.
 */
@Entity
@Getter
@Table(name = "PAYMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long id;

    @Column(name = "INVOICE_ID", nullable = false)
    private Long invoiceId;

    @Column(name = "PAID_DATE", nullable = false)
    private LocalDate paidDate;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "METHOD", length = 20)
    private String method;

    @Column(name = "BANK_REF", length = 100)
    private String bankRef;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Payment(Long invoiceId, LocalDate paidDate, BigDecimal amount, String currency,
                    String method, String bankRef) {
        this.invoiceId = invoiceId;
        this.paidDate = paidDate;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.bankRef = bankRef;
        this.createdAt = LocalDateTime.now();
    }
}
