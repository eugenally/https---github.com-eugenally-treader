package com.edu.bootstring.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByIdDesc();

    boolean existsByShipmentId(Long shipmentId);

    /** 견적 단계 PI 는 ORDER_ID 가 비어 있어 수주 취소를 막지 않는다 (D5-07 + D5-09) */
    boolean existsByOrderId(Long orderId);

    /**
     * 견적에 붙은 <b>PI</b>. 타입을 반드시 함께 걸러야 한다 —
     * CI 도 QUOTATION_ID 를 들고 있을 수 있어서, 타입을 빼면 CI 가 PI 발행을 막아버린다.
     *
     * <p>취소된 건은 제외한다. 잘못 발행해 취소한 뒤에는 다시 낼 수 있어야 하기 때문이다.
     */
    Optional<Invoice> findFirstByQuotationIdAndInvoiceTypeAndStatusNot(
            Long quotationId, String invoiceType, InvoiceStatus status);
}
