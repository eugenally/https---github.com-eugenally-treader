package com.edu.bootstring.order;

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
import jakarta.persistence.SequenceGenerator;
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
 * 수주. 견적에서 전환될 때 헤더와 라인을 <b>복사</b>한다(참조가 아니다).
 *
 * <p>견적서는 과거 시점의 증빙이므로, 견적이 나중에 수정돼도 수주 금액은 변하면 안 된다.
 * 그래서 단가까지 스냅샷으로 들고 온다.
 */
@Entity
@Getter
@Table(name = "SALES_ORDER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSalesOrder")
    @SequenceGenerator(name = "seqSalesOrder", sequenceName = "SEQ_SALES_ORDER", allocationSize = 1)
    @Column(name = "ORDER_ID")
    private Long id;

    @Column(name = "ORDER_NO", nullable = false, length = 40, updatable = false)
    private String orderNo;

    /** 견적 없이 바로 등록한 재주문이면 NULL (D5-02) */
    @Column(name = "QUOTATION_ID")
    private Long quotationId;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "ORDER_DATE", nullable = false)
    private LocalDate orderDate;

    @Column(name = "REQUIRED_DATE")
    private LocalDate requiredDate;

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

    @Column(name = "PAYMENT_DAYS")
    private Integer paymentDays;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "PO_NO", length = 50)
    private String poNo;

    @Column(name = "REMARK", length = 2000)
    private String remark;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<SalesOrderItem> items = new ArrayList<>();

    @Builder
    private SalesOrder(String orderNo, Long quotationId, Long customerId, LocalDate orderDate,
                       LocalDate requiredDate, String currency, String incoterms,
                       String portOfLoading, String portOfDischarge, String paymentTermsCode,
                       Integer paymentDays, String poNo, String remark) {
        this.orderNo = orderNo;
        this.quotationId = quotationId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.requiredDate = requiredDate;
        this.currency = currency;
        this.incoterms = incoterms;
        this.portOfLoading = portOfLoading;
        this.portOfDischarge = portOfDischarge;
        this.paymentTermsCode = paymentTermsCode;
        this.paymentDays = paymentDays;
        this.poNo = poNo;
        this.remark = remark;
        this.status = OrderStatus.CONFIRMED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(SalesOrderItem item) {
        item.assignTo(this);
        this.items.add(item);
    }

    public void recalcTotal() {
        this.totalAmount = this.items.stream()
                .map(SalesOrderItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void changeStatus(OrderStatus next) {
        this.status.validateTransitionTo(next);
        this.status = next;
    }

    /**
     * 출하 수량 변화에 따라 상태를 다시 판정한다.
     * 모든 라인이 주문량을 채웠으면 SHIPPED, 일부라도 나갔으면 PARTIALLY_SHIPPED.
     */
    public void recalcShippedStatus() {
        if (status == OrderStatus.CANCELLED || status == OrderStatus.CLOSED) {
            return;
        }
        boolean allShipped = items.stream().allMatch(SalesOrderItem::isFullyShipped);
        boolean anyShipped = items.stream().anyMatch(i -> i.getShippedQty().signum() > 0);

        if (allShipped) {
            changeStatus(OrderStatus.SHIPPED);
        } else if (anyShipped) {
            changeStatus(OrderStatus.PARTIALLY_SHIPPED);
        }
    }
}
