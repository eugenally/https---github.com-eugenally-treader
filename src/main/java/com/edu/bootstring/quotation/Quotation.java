package com.edu.bootstring.quotation;

import com.edu.bootstring.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 견적. 바이어에게 보내는 제안이며, 수락되면 수주로 복사된다.
 */
@Entity
@Getter
@Table(name = "QUOTATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quotation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QUOTATION_ID")
    private Long id;

    @Column(name = "QUOTE_NO", nullable = false, length = 40, updatable = false)
    private String quoteNo;

    @Column(name = "REV_NO", nullable = false)
    private Integer revNo;

    /** 개정 체인의 뿌리 (D5-03). Rev.0 이면 자기 자신이거나 NULL */
    @Column(name = "ORIGIN_QUOTATION_ID")
    private Long originQuotationId;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "QUOTE_DATE", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "VALID_UNTIL", nullable = false)
    private LocalDate validUntil;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "INCOTERMS", length = 10)
    private String incoterms;

    @Column(name = "PORT_OF_LOADING", length = 100)
    private String portOfLoading;

    @Column(name = "PORT_OF_DISCHARGE", length = 100)
    private String portOfDischarge;

    @Column(name = "PAYMENT_TERMS_CODE", length = 20)
    private String paymentTermsCode;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /** 작성 시점의 매매기준율 스냅샷 — 원화 매출 통계의 기준이 된다 */
    @Column(name = "EXCHANGE_RATE", precision = 15, scale = 4)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private QuotationStatus status;

    @Column(name = "REMARK", length = 2000)
    private String remark;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<QuotationItem> items = new ArrayList<>();

    @Builder
    private Quotation(String quoteNo, Long customerId, LocalDate quoteDate, LocalDate validUntil,
                      String currency, String incoterms, String portOfLoading, String portOfDischarge,
                      String paymentTermsCode, BigDecimal exchangeRate, String remark) {
        this.quoteNo = quoteNo;
        this.revNo = 0;
        this.customerId = customerId;
        this.quoteDate = quoteDate;
        this.validUntil = validUntil;
        this.currency = currency;
        this.incoterms = incoterms;
        this.portOfLoading = portOfLoading;
        this.portOfDischarge = portOfDischarge;
        this.paymentTermsCode = paymentTermsCode;
        this.exchangeRate = exchangeRate;
        this.remark = remark;
        this.status = QuotationStatus.DRAFT;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(QuotationItem item) {
        item.assignTo(this);
        this.items.add(item);
    }

    public void clearItems() {
        this.items.clear();
    }

    public void recalcTotal() {
        this.totalAmount = this.items.stream()
                .map(QuotationItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void changeStatus(QuotationStatus next) {
        this.status.validateTransitionTo(next);
        this.status = next;
    }

    /** 원화 환산액. 환율 스냅샷이 없으면 null 을 돌려준다. */
    public BigDecimal getKrwAmount() {
        if (exchangeRate == null) {
            return null;
        }
        return totalAmount.multiply(exchangeRate).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    public boolean isEditable() {
        return status == QuotationStatus.DRAFT || status == QuotationStatus.SENT;
    }
}
