package com.edu.bootstring.invoice;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.exchange.ExchangeRate;
import com.edu.bootstring.exchange.ExchangeRateRepository;
import com.edu.bootstring.global.docnumber.DocNumberService;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.invoice.dto.InvoiceDtos;
import com.edu.bootstring.order.SalesOrder;
import com.edu.bootstring.order.SalesOrderItem;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.quotation.Quotation;
import com.edu.bootstring.quotation.QuotationRepository;
import com.edu.bootstring.quotation.QuotationStatus;
import com.edu.bootstring.shipment.Shipment;
import com.edu.bootstring.shipment.ShipmentItem;
import com.edu.bootstring.shipment.ShipmentRepository;
import com.edu.bootstring.shipment.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인보이스 발행과 입금 처리.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final int PI_DUE_DAYS = 7;

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final DocNumberService docNumberService;

    @Transactional(readOnly = true)
    public List<InvoiceDtos.Response> findAll() {
        return invoiceRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.Response findOne(Long id) {
        return toResponse(get(id));
    }

    /**
     * 출하 확정분에 대한 CI 발행.
     *
     * <p>금액은 출하 수량 × 수주 단가로 계산한다. 환율은 <b>선적일(ETD)</b> 기준으로 고정하고(D5-14),
     * 결제기한은 <b>B/L date + 신용일수</b>로 잡는다(D5-11).
     * 실무 계약 문구가 "30 days after B/L date"라 발행일 기준으로 하면 며칠씩 어긋난다.
     */
    @Transactional
    public InvoiceDtos.Response issueCommercialInvoice(InvoiceDtos.IssueCiRequest req) {
        Shipment shipment = shipmentRepository.findById(req.shipmentId())
                .orElseThrow(() -> new NotFoundException("출하를 찾을 수 없습니다. id=" + req.shipmentId()));

        if (shipment.getStatus() != ShipmentStatus.SHIPPED && shipment.getStatus() != ShipmentStatus.ARRIVED) {
            throw new BusinessException(
                    "확정된 출하에만 CI 를 발행할 수 있습니다. 현재 상태=" + shipment.getStatus(),
                    ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (invoiceRepository.existsByShipmentId(shipment.getId())) {
            throw new BusinessException("이미 CI 가 발행된 출하입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        SalesOrder order = salesOrderRepository.findById(shipment.getOrderId())
                .orElseThrow(() -> new NotFoundException("수주를 찾을 수 없습니다."));
        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다."));

        BigDecimal amount = calculateShipmentAmount(shipment, order);

        LocalDate issueDate = req.issueDate() != null ? req.issueDate() : LocalDate.now();
        LocalDate blDate = shipment.getShipDate() != null ? shipment.getShipDate() : issueDate;
        int creditDays = order.getPaymentDays() != null ? order.getPaymentDays() : 0;

        LocalDate rateBaseDate = shipment.getEtd() != null ? shipment.getEtd() : issueDate;
        BigDecimal rate = findRate(order.getCurrency(), rateBaseDate).orElse(null);

        Invoice invoice = Invoice.builder()
                .invoiceNo(docNumberService.next("CI", customer.getCustomerCode()))
                .invoiceType("CI")
                .orderId(order.getId())
                .shipmentId(shipment.getId())
                .issueDate(issueDate)
                .dueDate(blDate.plusDays(creditDays))
                .currency(order.getCurrency())
                .amount(amount)
                .rateBaseDate(rateBaseDate)
                .exchangeRate(rate)
                .paymentTermsCode(order.getPaymentTermsCode())
                .build();

        return toResponse(invoiceRepository.save(invoice));
    }

    /**
     * 견적 단계 선금 PI 발행 (D5-09).
     * 금액은 견적 총액 × 거래처 선금 비율, 결제기한은 발행일 + 7일이다.
     */
    @Transactional
    public InvoiceDtos.Response issueProformaInvoice(InvoiceDtos.IssuePiRequest req) {
        Quotation quotation = quotationRepository.findById(req.quotationId())
                .orElseThrow(() -> new NotFoundException("견적을 찾을 수 없습니다. id=" + req.quotationId()));
        Customer customer = customerRepository.findById(quotation.getCustomerId())
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다."));

        if (quotation.getItems().isEmpty()) {
            throw new BusinessException("품목이 없는 견적에는 PI 를 발행할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        // DRAFT 는 아직 바이어에게 나가지 않은 견적이다. 선금을 청구할 상대가 없다.
        if (quotation.getStatus() == QuotationStatus.DRAFT) {
            throw new BusinessException(
                    "발송하지 않은 견적에는 PI 를 발행할 수 없습니다. 먼저 발송하세요.",
                    ErrorCode.INVALID_STATE_TRANSITION);
        }
        invoiceRepository.findFirstByQuotationIdAndInvoiceTypeAndStatusNot(
                        quotation.getId(), "PI", InvoiceStatus.CANCELLED)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "이미 PI 가 발행된 견적입니다. (%s)".formatted(existing.getInvoiceNo()),
                            ErrorCode.INVALID_INPUT_VALUE);
                });

        BigDecimal advanceRate = customer.getAdvanceRate() != null
                ? customer.getAdvanceRate()
                : BigDecimal.valueOf(30);
        // 선금 비율 0% 인 거래처(전액 후불)는 청구할 선금이 없다.
        // 막지 않으면 금액 0 짜리 인보이스가 만들어진다.
        if (advanceRate.signum() <= 0) {
            throw new BusinessException(
                    "%s 는 선금 비율이 0%% 라 PI 를 발행할 수 없습니다.".formatted(customer.getNameEn()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        BigDecimal amount = quotation.getTotalAmount()
                .multiply(advanceRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new BusinessException("PI 청구 금액이 0 입니다. 견적 금액을 확인하세요.",
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate issueDate = req.issueDate() != null ? req.issueDate() : LocalDate.now();
        BigDecimal rate = findRate(quotation.getCurrency(), issueDate).orElse(quotation.getExchangeRate());

        Invoice invoice = Invoice.builder()
                .invoiceNo(docNumberService.next("PI", customer.getCustomerCode()))
                .invoiceType("PI")
                .quotationId(quotation.getId())
                .issueDate(issueDate)
                .dueDate(issueDate.plusDays(PI_DUE_DAYS))
                .currency(quotation.getCurrency())
                .amount(amount)
                .rateBaseDate(issueDate)
                .exchangeRate(rate)
                .paymentTermsCode(quotation.getPaymentTermsCode())
                .build();

        return toResponse(invoiceRepository.save(invoice));
    }

    /** 입금 등록. 누계로 인보이스 상태가 자동 판정된다. */
    @Transactional
    public InvoiceDtos.Response registerPayment(Long invoiceId, InvoiceDtos.PaymentRequest req) {
        Invoice invoice = get(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("취소된 인보이스에는 입금을 등록할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        if (req.amount().compareTo(invoice.getBalance()) > 0) {
            throw new BusinessException(
                    "입금액이 미수 잔액을 초과합니다. 잔액 %s, 요청 %s".formatted(invoice.getBalance(), req.amount()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        paymentRepository.save(Payment.builder()
                .invoiceId(invoice.getId())
                .paidDate(req.paidDate() != null ? req.paidDate() : LocalDate.now())
                .amount(req.amount())
                .currency(invoice.getCurrency())
                .method(req.method())
                .bankRef(req.bankRef())
                .build());

        BigDecimal paidTotal = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        invoice.applyPayment(paidTotal, LocalDate.now());

        return toResponse(invoice);
    }

    /** 출하 수량 × 수주 단가 */
    private BigDecimal calculateShipmentAmount(Shipment shipment, SalesOrder order) {
        Map<Long, SalesOrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        return shipment.getItems().stream()
                .map(si -> {
                    SalesOrderItem oi = orderItems.get(si.getOrderItemId());
                    if (oi == null) {
                        throw new NotFoundException("수주 품목을 찾을 수 없습니다. id=" + si.getOrderItemId());
                    }
                    return si.getQty().multiply(oi.getUnitPrice());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<BigDecimal> findRate(String currency, LocalDate baseDate) {
        return exchangeRateRepository
                .findByCurUnitAndBaseDateLessThanEqualOrderByBaseDateDesc(currency, baseDate, Limit.of(1))
                .stream()
                .findFirst()
                .map(ExchangeRate::getDealBasR);
    }

    Invoice get(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("인보이스를 찾을 수 없습니다. id=" + id));
    }

    InvoiceDtos.Response toResponse(Invoice inv) {
        SalesOrder order = inv.getOrderId() == null
                ? null
                : salesOrderRepository.findById(inv.getOrderId()).orElse(null);
        Quotation quotation = inv.getQuotationId() == null
                ? null
                : quotationRepository.findById(inv.getQuotationId()).orElse(null);
        Shipment shipment = inv.getShipmentId() == null
                ? null
                : shipmentRepository.findById(inv.getShipmentId()).orElse(null);

        Long customerId = order != null ? order.getCustomerId()
                : quotation != null ? quotation.getCustomerId() : null;
        String customerName = customerId == null
                ? null
                : customerRepository.findById(customerId).map(Customer::getNameEn).orElse(null);

        List<InvoiceDtos.PaymentResponse> payments = paymentRepository
                .findAllByInvoiceIdOrderByPaidDateAsc(inv.getId()).stream()
                .map(p -> new InvoiceDtos.PaymentResponse(
                        p.getId(), p.getPaidDate(), p.getAmount(), p.getMethod(), p.getBankRef()))
                .toList();

        return new InvoiceDtos.Response(
                inv.getId(),
                inv.getInvoiceNo(),
                inv.getInvoiceType(),
                inv.getQuotationId(),
                inv.getOrderId(),
                order != null ? order.getOrderNo() : null,
                inv.getShipmentId(),
                shipment != null ? shipment.getShipmentNo() : null,
                customerId,
                customerName,
                inv.getStatus().name(),
                inv.getCurrency(),
                inv.getAmount(),
                inv.getPaidAmount(),
                inv.getBalance(),
                inv.getIssueDate(),
                inv.getDueDate(),
                inv.getRateBaseDate(),
                inv.getExchangeRate(),
                inv.getKrwAmount(),
                payments);
    }
}
