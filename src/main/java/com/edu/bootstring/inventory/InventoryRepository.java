package com.edu.bootstring.inventory;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 재고 차감 전에 행을 잠근다. {@code SELECT ... FOR UPDATE WAIT 3} 로 번역된다.
     *
     * <p>이 락이 없으면 동시 출하 확정에서 Lost Update 가 난다:
     * A와 B가 각각 재고 100을 읽고 둘 다 60 출하를 통과시켜, 120이 나갔는데 재고는 40이 된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select i from Inventory i where i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

    List<Inventory> findAllByProductIdIn(List<Long> productIds);
}
