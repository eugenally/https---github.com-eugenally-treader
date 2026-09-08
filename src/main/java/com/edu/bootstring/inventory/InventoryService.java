package com.edu.bootstring.inventory;

import com.edu.bootstring.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 재고 변동을 담당한다. 모든 변경은 행 잠금 뒤에 일어난다.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /** 수주 확정 시 할당. 실물은 그대로 두고 예약만 잡는다 (D5-04). */
    @Transactional
    public void allocate(Long productId, BigDecimal qty, Long orderId) {
        Inventory inv = lock(productId);
        BigDecimal before = inv.getAllocatedQty();

        inv.allocate(qty);

        inventoryTxnRepository.save(InventoryTxn.builder()
                .productId(productId)
                .txnType("ALLOC")
                .qty(qty)
                .beforeQty(before)
                .afterQty(inv.getAllocatedQty())
                .refType("SALES_ORDER")
                .refId(orderId)
                .build());
    }

    /** 수주 취소 시 할당 해제 */
    @Transactional
    public void release(Long productId, BigDecimal qty, Long orderId) {
        Inventory inv = lock(productId);
        BigDecimal before = inv.getAllocatedQty();

        inv.release(qty);

        inventoryTxnRepository.save(InventoryTxn.builder()
                .productId(productId)
                .txnType("RELEASE")
                .qty(qty.negate())
                .beforeQty(before)
                .afterQty(inv.getAllocatedQty())
                .refType("SALES_ORDER")
                .refId(orderId)
                .build());
    }

    /**
     * 출하 확정 시 실물 차감.
     *
     * <p>여기가 이 프로젝트의 동시성 급소다. {@code lock()} 이 없으면
     * 두 사용자가 같은 재고를 각각 읽고 둘 다 검증을 통과해 재고가 음수로 내려간다.
     */
    @Transactional
    public void deduct(Long productId, BigDecimal qty, Long shipmentId) {
        Inventory inv = lock(productId);
        BigDecimal before = inv.getOnHandQty();

        inv.deduct(qty);

        inventoryTxnRepository.save(InventoryTxn.builder()
                .productId(productId)
                .txnType("OUT")
                .qty(qty.negate())
                .beforeQty(before)
                .afterQty(inv.getOnHandQty())
                .refType("SHIPMENT")
                .refId(shipmentId)
                .build());
    }

    private Inventory lock(Long productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("재고 정보가 없습니다. productId=" + productId));
    }
}
