package com.edu.bootstring.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findAllByOrderByIdDesc();

    List<Shipment> findAllByOrderIdOrderByIdAsc(Long orderId);

    boolean existsByIdAndStatus(Long id, ShipmentStatus status);
}
