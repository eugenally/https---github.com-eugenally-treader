package com.edu.bootstring.order;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findAllByOrderByIdDesc();

    boolean existsByQuotationId(Long quotationId);

    long countByCustomerId(Long customerId);

    @Query("select o.customerId, count(o) from SalesOrder o group by o.customerId")
    List<Object[]> countGroupedByCustomer();

    /**
     * 출하 확정 전에 수주 행을 잠근다.
     *
     * <p>같은 수주에 걸린 출하 두 건을 동시에 확정하면 {@code SALES_ORDER_ITEM.SHIPPED_QTY} 에
     * Lost Update 가 난다. 두 트랜잭션이 각자 shippedQty=0 을 읽고 각자 60 을 더해 둘 다 60 을 쓰면,
     * 실제로는 120 이 나갔는데 장부에는 60 만 찍힌다. 재고는 정확히 120 줄어 있으므로
     * 수주와 재고가 어긋난다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select o from SalesOrder o where o.id = :id")
    Optional<SalesOrder> findByIdForUpdate(@Param("id") Long id);
}
