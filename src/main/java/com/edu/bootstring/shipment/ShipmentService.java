package com.edu.bootstring.shipment;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.docnumber.DocNumberService;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.inventory.InventoryService;
import com.edu.bootstring.invoice.InvoiceRepository;
import com.edu.bootstring.order.SalesOrder;
import com.edu.bootstring.order.SalesOrderItem;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import com.edu.bootstring.shipment.dto.ShipmentDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 출하 등록과 확정. 재고가 실제로 움직이는 유일한 지점이다.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryService inventoryService;
    private final DocNumberService docNumberService;

    @Transactional(readOnly = true)
    public List<ShipmentDtos.SummaryResponse> findAll() {
        Map<Long, SalesOrder> orders = orderMap();
        Map<Long, Customer> customers = customerMap();

        return shipmentRepository.findAllByOrderByIdDesc().stream()
                .map(s -> {
                    SalesOrder o = orders.get(s.getOrderId());
                    return new ShipmentDtos.SummaryResponse(
                            s.getId(),
                            s.getShipmentNo(),
                            s.getOrderId(),
                            o != null ? o.getOrderNo() : null,
                            o != null ? customerName(customers, o.getCustomerId()) : null,
                            s.getStatus().name(),
                            s.getTransportMode(),
                            s.getEtd(),
                            s.getShipDate(),
                            s.getBlNo(),
                            s.getTotalGrossWeight(),
                            s.isDeletable(),
                            invoiceRepository.existsByShipmentId(s.getId()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentDtos.DetailResponse findDetail(Long id) {
        return toDetail(get(id));
    }

    /**
     * 출하 등록. PLANNED 로 만들어지며 <b>재고는 아직 건드리지 않는다</b>.
     *
     * <p>PLANNED 출하는 삭제할 수 있어야 하는데(D5-08), 여기서 재고를 차감하면
     * 삭제할 때마다 되돌려야 하고 그 사이 다른 출하가 끼어들면 정합성이 깨진다.
     */
    @Transactional
    public ShipmentDtos.DetailResponse create(ShipmentDtos.CreateRequest req) {
        SalesOrder order = salesOrderRepository.findById(req.orderId())
                .orElseThrow(() -> new NotFoundException("수주를 찾을 수 없습니다. id=" + req.orderId()));

        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다."));

        Map<Long, SalesOrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        Shipment shipment = Shipment.builder()
                .shipmentNo(docNumberService.next("SH", customer.getCustomerCode()))
                .orderId(order.getId())
                .transportMode(req.transportMode())
                .etd(req.etd())
                .eta(req.eta())
                .vesselName(req.vesselName())
                .containerType(req.containerType())
                .containerNo(req.containerNo())
                .portOfLoading(orDefault(req.portOfLoading(), order.getPortOfLoading()))
                .portOfDischarge(orDefault(req.portOfDischarge(), order.getPortOfDischarge()))
                .totalCbm(req.totalCbm())
                .totalCarton(req.totalCarton())
                .remark(req.remark())
                .build();

        for (ShipmentDtos.CreateItemRequest itemReq : req.items()) {
            SalesOrderItem orderItem = orderItems.get(itemReq.orderItemId());
            if (orderItem == null) {
                throw new NotFoundException("수주 품목을 찾을 수 없습니다. id=" + itemReq.orderItemId());
            }
            // 잔량 초과 여부는 여기서 미리 막는다. 확정 때 다시 한 번 검증된다.
            if (itemReq.qty().compareTo(orderItem.getPendingQty()) > 0) {
                throw new BusinessException(
                        "출하 수량이 잔량을 초과합니다. 품목 %d, 잔량 %s, 요청 %s"
                                .formatted(orderItem.getLineNo(), orderItem.getPendingQty(), itemReq.qty()),
                        ErrorCode.INVALID_INPUT_VALUE);
            }

            shipment.addItem(ShipmentItem.builder()
                    .orderItemId(orderItem.getId())
                    .productId(orderItem.getProductId())
                    .qty(itemReq.qty())
                    .cartonQty(itemReq.cartonQty())
                    .netWeight(itemReq.netWeight())
                    .grossWeight(itemReq.grossWeight())
                    .cbm(itemReq.cbm())
                    .build());
        }
        shipment.recalcWeights();

        return toDetail(shipmentRepository.save(shipment));
    }

    /**
     * 출하 확정. <b>여기서 실물 재고가 빠진다</b> (D5-08).
     *
     * <p>여러 품목을 차감할 때 productId 오름차순으로 잠근다.
     * A가 1→2 순서로, B가 2→1 순서로 잠그면 서로를 기다리며 영원히 멈춘다. 정렬 한 줄이 그걸 막는다.
     */
    @Transactional
    public ShipmentDtos.DetailResponse confirm(Long shipmentId, ShipmentDtos.ConfirmRequest req) {
        Shipment shipment = get(shipmentId);

        // 잠그는 순서는 항상 수주 → 재고(제품 오름차순)다. 이 순서를 모든 경로에서 지켜야
        // 서로 다른 자원을 반대 순서로 잡는 데드락이 생기지 않는다.
        SalesOrder order = salesOrderRepository.findByIdForUpdate(shipment.getOrderId())
                .orElseThrow(() -> new NotFoundException("수주를 찾을 수 없습니다."));

        applyTransportDoc(shipment, req);
        if (!shipment.hasTransportDocNo()) {
            throw new BusinessException(
                    "AIR".equals(shipment.getTransportMode())
                            ? "항공 출하는 AWB 번호가 있어야 확정할 수 있습니다."
                            : "해상 출하는 B/L 번호가 있어야 확정할 수 있습니다.",
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        shipment.markShipped(req.shipDate() != null ? req.shipDate() : LocalDate.now());

        Map<Long, SalesOrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        shipment.getItems().stream()
                .sorted(Comparator.comparing(ShipmentItem::getProductId))   // ★ 데드락 회피
                .forEach(si -> {
                    SalesOrderItem oi = orderItems.get(si.getOrderItemId());
                    if (oi == null) {
                        throw new NotFoundException("수주 품목을 찾을 수 없습니다. id=" + si.getOrderItemId());
                    }
                    oi.addShippedQty(si.getQty());                          // 잔량 검증 포함
                    inventoryService.deduct(si.getProductId(), si.getQty(), shipment.getId());
                });

        order.recalcShippedStatus();

        return toDetail(shipment);
    }

    /** PLANNED 출하만 지울 수 있다. 재고를 아직 안 건드렸으니 되돌릴 것도 없다. */
    @Transactional
    public void delete(Long shipmentId) {
        Shipment shipment = get(shipmentId);
        if (!shipment.isDeletable()) {
            throw new BusinessException(
                    "%s 상태의 출하는 삭제할 수 없습니다. PLANNED 상태에서만 가능합니다."
                            .formatted(shipment.getStatus()),
                    ErrorCode.INVALID_STATE_TRANSITION);
        }
        shipmentRepository.delete(shipment);
    }

    private void applyTransportDoc(Shipment shipment, ShipmentDtos.ConfirmRequest req) {
        if (req.blNo() != null && !req.blNo().isBlank()) {
            shipment.assignBlNo(req.blNo());
        }
        if (req.awbNo() != null && !req.awbNo().isBlank()) {
            shipment.assignAwbNo(req.awbNo());
        }
    }

    Shipment get(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("출하를 찾을 수 없습니다. id=" + id));
    }

    ShipmentDtos.DetailResponse toDetail(Shipment s) {
        Map<Long, Product> products = productMap();
        SalesOrder order = salesOrderRepository.findById(s.getOrderId()).orElse(null);
        Map<Long, Customer> customers = customerMap();

        List<ShipmentDtos.ItemResponse> items = s.getItems().stream()
                .map(i -> {
                    Product p = products.get(i.getProductId());
                    return new ShipmentDtos.ItemResponse(
                            i.getId(),
                            i.getOrderItemId(),
                            i.getProductId(),
                            p != null ? p.getProductCode() : null,
                            p != null ? p.getNameEn() : null,
                            i.getQty(),
                            i.getCartonQty(),
                            i.getNetWeight(),
                            i.getGrossWeight());
                })
                .toList();

        return new ShipmentDtos.DetailResponse(
                s.getId(),
                s.getShipmentNo(),
                s.getOrderId(),
                order != null ? order.getOrderNo() : null,
                order != null ? customerName(customers, order.getCustomerId()) : null,
                s.getStatus().name(),
                s.getTransportMode(),
                s.getEtd(),
                s.getEta(),
                s.getShipDate(),
                s.getVesselName(),
                s.getBlNo(),
                s.getAwbNo(),
                s.getContainerType(),
                s.getContainerNo(),
                s.getPortOfLoading(),
                s.getPortOfDischarge(),
                s.getTotalNetWeight(),
                s.getTotalGrossWeight(),
                s.getTotalCbm(),
                s.getTotalCarton(),
                s.isDeletable(),
                invoiceRepository.existsByShipmentId(s.getId()),
                items);
    }

    private Map<Long, SalesOrder> orderMap() {
        return salesOrderRepository.findAll().stream()
                .collect(Collectors.toMap(SalesOrder::getId, Function.identity()));
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

    private String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
