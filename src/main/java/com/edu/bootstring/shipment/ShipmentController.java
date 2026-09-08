package com.edu.bootstring.shipment;

import com.edu.bootstring.shipment.dto.ShipmentDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public List<ShipmentDtos.SummaryResponse> findAll() {
        return shipmentService.findAll();
    }

    @GetMapping("/{id}")
    public ShipmentDtos.DetailResponse findOne(@PathVariable Long id) {
        return shipmentService.findDetail(id);
    }

    /** 출하 등록 — PLANNED 로 만들어지고 재고는 아직 움직이지 않는다 */
    @PostMapping
    public ShipmentDtos.DetailResponse create(@Valid @RequestBody ShipmentDtos.CreateRequest req) {
        return shipmentService.create(req);
    }

    /** 출하 확정 — 실물 재고가 차감되는 지점 */
    @PostMapping("/{id}/confirm")
    public ShipmentDtos.DetailResponse confirm(@PathVariable Long id,
                                               @RequestBody(required = false) ShipmentDtos.ConfirmRequest req) {
        ShipmentDtos.ConfirmRequest safe = req != null
                ? req
                : new ShipmentDtos.ConfirmRequest(null, null, null);
        return shipmentService.confirm(id, safe);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
