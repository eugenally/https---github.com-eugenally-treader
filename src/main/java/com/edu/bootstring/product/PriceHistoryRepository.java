package com.edu.bootstring.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /**
     * 기준일에 유효한 단가를 우선순위 순으로 조회한다.
     * 거래처 전용 단가가 먼저 오고, 없으면 표준 단가(CUSTOMER_ID IS NULL)가 뒤따른다.
     * 첫 번째 행이 곧 적용 단가다.
     */
    @Query("""
            select p from PriceHistory p
             where p.productId = :productId
               and (p.customerId = :customerId or p.customerId is null)
               and p.validFrom <= :baseDate
               and (p.validTo is null or p.validTo >= :baseDate)
             order by case when p.customerId is null then 1 else 0 end asc,
                      p.validFrom desc
            """)
    List<PriceHistory> findApplicable(@Param("productId") Long productId,
                                      @Param("customerId") Long customerId,
                                      @Param("baseDate") LocalDate baseDate);
}
