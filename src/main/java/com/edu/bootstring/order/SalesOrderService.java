package com.edu.bootstring.order;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.docnumber.DocNumberService;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import com.edu.bootstring.inventory.InventoryService;
import com.edu.bootstring.invoice.InvoiceRepository;
import com.edu.bootstring.order.dto.OrderDtos;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import com.edu.bootstring.quotation.Quotation;
import com.edu.bootstring.quotation.QuotationItem;
import com.edu.bootstring.quotation.QuotationRepository;
import com.edu.bootstring.quotation.QuotationStatus;
import com.edu.bootstring.shipment.Shipment;
import com.edu.bootstring.shipment.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 수주 조회와 견적 전환을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ShipmentRepository shipmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final DocNumberService docNumberService;

    @Transactional(readOnly = true)
    public List<OrderDtos.SummaryResponse> findAll() {
        Map<Long, Customer> customers = customerMap();

        return salesOrderRepository.findAllByOrderByIdDesc().stream()
                .map(o -> new OrderDtos.SummaryResponse(
                        o.getId(),
                        o.getOrderNo(),
                        o.getCustomerId(),
                        customerName(customers, o.getCustomerId()),
                        o.getStatus().name(),
                        o.getCurrency(),
                        o.getTotalAmount(),
                        o.getOrderDate(),
                        o.getRequiredDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.DetailResponse findDetail(Long id) {
        return toDetail(get(id));
    }

    /**
     * 견적 → 수주 전환. 이 메서드 하나가 트랜잭션 경계다.
     *
     * <p>중간에 예외가 나면 견적 상태도, 새로 만든 수주도, 재고 할당도 전부 함께 롤백된다.
     * 라인은 참조가 아니라 <b>복사</b>한다. 견적이 나중에 바뀌어도 수주 금액은 그대로여야 하기 때문이다.
     */
    @Transactional
    public OrderDtos.DetailResponse convertFromQuotation(Long quotationId, OrderDtos.ConvertRequest req) {

        // 1) 견적 행을 잠근다 — 두 사람이 동시에 전환 버튼을 눌러도 하나만 통과한다
        Quotation quotation = quotationRepository.findByIdForUpdate(quotationId)
                .orElseThrow(() -> new NotFoundException("견적을 찾을 수 없습니다. id=" + quotationId));

        // 2) 전환 가능 여부 검증
        if (quotation.getStatus() != QuotationStatus.SENT) {
            throw new BusinessException(
                    "발송된 견적만 수주로 전환할 수 있습니다. 현재 상태=" + quotation.getStatus(),
                    ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (salesOrderRepository.existsByQuotationId(quotationId)) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CONVERTED);
        }
        if (quotation.getItems().isEmpty()) {
            throw new BusinessException("품목이 없는 견적은 전환할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        Customer customer = customerRepository.findById(quotation.getCustomerId())
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다."));

        // 3) 견적을 수락 상태로
        quotation.changeStatus(QuotationStatus.ACCEPTED);

        // 4) 수주 생성 — 헤더와 라인을 스냅샷으로 복사한다
        SalesOrder order = SalesOrder.builder()
                .orderNo(docNumberService.next("SO", customer.getCustomerCode()))
                .quotationId(quotation.getId())
                .customerId(quotation.getCustomerId())
                .orderDate(LocalDate.now())
                .requiredDate(req.requiredDate())
                .currency(quotation.getCurrency())
                .incoterms(quotation.getIncoterms())
                .portOfLoading(quotation.getPortOfLoading())
                .portOfDischarge(quotation.getPortOfDischarge())
                .paymentTermsCode(quotation.getPaymentTermsCode())
                .paymentDays(customer.getPaymentDays())
                .poNo(req.poNo())
                .remark(req.remark())
                .build();

        int lineNo = 1;
        for (QuotationItem qi : quotation.getItems()) {
            order.addItem(SalesOrderItem.builder()
                    .lineNo(lineNo++)
                    .productId(qi.getProductId())
                    .orderedQty(qi.getQty())
                    .unitPrice(qi.getUnitPrice())   // ← 가격 스냅샷
                    .remark(qi.getRemark())
                    .build());
        }
        order.recalcTotal();
        SalesOrder saved = salesOrderRepository.save(order);

        // 5) 재고 할당 (D5-04). 실물은 아직 나가지 않으므로 allocated 만 늘어난다.
        //    데드락을 피하려고 항상 productId 오름차순으로 잠근다.
        saved.getItems().stream()
                .sorted(java.util.Comparator.comparing(SalesOrderItem::getProductId))
                .forEach(i -> inventoryService.allocate(i.getProductId(), i.getOrderedQty(), saved.getId()));

        return toDetail(saved);
    }

    /** 출하 등록 화면이 쓰는 잔량 목록 */
    @Transactional(readOnly = true)
    public List<OrderDtos.PendingItemResponse> findPendingItems(Long orderId) {
        SalesOrder order = get(orderId);
        Map<Long, Product> products = productMap();
        Map<Long, Inventory> inventories = inventoryRepository
                .findAllByProductIdIn(order.getItems().stream().map(SalesOrderItem::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        return order.getItems().stream()
                .filter(i -> i.getPendingQty().signum() > 0)
                .map(i -> {
                    Product p = products.get(i.getProductId());
                    Inventory inv = inventories.get(i.getProductId());
                    return new OrderDtos.PendingItemResponse(
                            i.getId(),
                            i.getLineNo(),
                            i.getProductId(),
                            p != null ? p.getProductCode() : null,
                            p != null ? p.getNameEn() : null,
                            p != null ? p.getUnit() : null,
                            i.getOrderedQty(),
                            i.getShippedQty(),
                            i.getPendingQty(),
                            p != null ? p.getNetWeight() : null,
                            p != null ? p.getGrossWeight() : null,
                            inv != null ? inv.getOnHandQty() : null);
                })
                .toList();
    }

    SalesOrder get(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("수주를 찾을 수 없습니다. id=" + id));
    }

    OrderDtos.DetailResponse toDetail(SalesOrder o) {
        Map<Long, Customer> customers = customerMap();
        Map<Long, Product> products = productMap();

        List<OrderDtos.ItemResponse> items = o.getItems().stream()
                .map(i -> {
                    Product p = products.get(i.getProductId());
                    return new OrderDtos.ItemResponse(
                            i.getId(),
                            i.getLineNo(),
                            i.getProductId(),
                            p != null ? p.getProductCode() : null,
                            p != null ? p.getNameEn() : null,
                            p != null ? p.getUnit() : null,
                            i.getOrderedQty(),
                            i.getShippedQty(),
                            i.getPendingQty(),
                            i.getUnitPrice(),
                            i.getAmount(),
                            percent(i.getShippedQty(), i.getOrderedQty()));
                })
                .toList();

        List<OrderDtos.ShipmentSummary> shipments = shipmentRepository
                .findAllByOrderIdOrderByIdAsc(o.getId()).stream()
                .map(s -> new OrderDtos.ShipmentSummary(
                        s.getId(),
                        s.getShipmentNo(),
                        s.getStatus().name(),
                        s.getTransportMode(),
                        s.getEtd(),
                        s.getShipDate(),
                        s.getContainerType(),
                        s.getBlNo(),
                        s.getTotalGrossWeight()))
                .toList();

        String quoteNo = o.getQuotationId() == null
                ? null
                : quotationRepository.findById(o.getQuotationId()).map(Quotation::getQuoteNo).orElse(null);

        return new OrderDtos.DetailResponse(
                o.getId(),
                o.getOrderNo(),
                o.getQuotationId(),
                quoteNo,
                o.getCustomerId(),
                customerName(customers, o.getCustomerId()),
                o.getStatus().name(),
                o.getCurrency(),
                o.getIncoterms(),
                o.getPortOfLoading(),
                o.getPortOfDischarge(),
                o.getOrderDate(),
                o.getRequiredDate(),
                o.getTotalAmount(),
                o.getPoNo(),
                o.getRemark(),
                isCancellable(o),
                items,
                shipments);
    }

    /**
     * 취소 가능 여부 (D5-07).
     * 견적 단계 PI 는 ORDER_ID 가 비어 있으므로 취소를 막지 않는다 — 안 그러면
     * 선금 청구서를 먼저 뽑은 건은 영원히 취소가 안 된다.
     */
    private boolean isCancellable(SalesOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.CLOSED) {
            return false;
        }
        boolean hasShipment = shipmentRepository.findAllByOrderIdOrderByIdAsc(order.getId()).stream()
                .anyMatch(s -> s.getStatus() != com.edu.bootstring.shipment.ShipmentStatus.CANCELLED);
        return !hasShipment && !invoiceRepository.existsByOrderId(order.getId());
    }

    private int percent(BigDecimal shipped, BigDecimal ordered) {
        if (ordered == null || ordered.signum() == 0) {
            return 0;
        }
        return shipped.multiply(BigDecimal.valueOf(100))
                .divide(ordered, 0, RoundingMode.DOWN)
                .intValue();
    }

    private Map<Long, Customer> customerMap() {
        return customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private Map<Long, Product> productMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String customerName(Map<Long, Customer> customers, Long customerId) {
        Customer c = customers.get(customerId);
        return c != null ? c.getNameEn() : null;
    }
}
