package com.edu.bootstring.quotation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findAllByOrderByIdDesc();

    long countByCustomerId(Long customerId);

    /** 거래처 목록 화면에서 쓸 건수 집계. 행마다 세면 N+1 이 된다. */
    @Query("select q.customerId, count(q) from Quotation q group by q.customerId")
    List<Object[]> countGroupedByCustomer();

    /** 견적 → 수주 전환 시 행을 잠가 동시 전환을 막는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.id = :id")
    Optional<Quotation> findByIdForUpdate(@Param("id") Long id);
}
