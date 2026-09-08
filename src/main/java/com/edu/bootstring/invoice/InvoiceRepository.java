package com.edu.bootstring.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByIdDesc();

    boolean existsByShipmentId(Long shipmentId);

    /** 견적 단계 PI 는 ORDER_ID 가 비어 있어 수주 취소를 막지 않는다 (D5-07 + D5-09) */
    boolean existsByOrderId(Long orderId);
}
