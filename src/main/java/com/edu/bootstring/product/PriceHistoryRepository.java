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

    /**
     * 같은 (거래처, 제품, 통화) 조합의 단가 이력.
     * 기간 겹침 검사와 이전 단가 닫기에 쓴다. {@code customerId} 가 null 이면 표준 단가 줄이다.
     */
    @Query("""
            select p from PriceHistory p
             where p.productId = :productId
               and ((:customerId is null and p.customerId is null) or p.customerId = :customerId)
             order by p.validFrom asc
            """)
    List<PriceHistory> findSeries(@Param("productId") Long productId,
                                  @Param("customerId") Long customerId);

    @Query("""
            select p from PriceHistory p
             where (:productId is null or p.productId = :productId)
               and (:customerId is null or p.customerId = :customerId)
             order by p.productId asc, p.validFrom desc
            """)
    List<PriceHistory> findForList(@Param("productId") Long productId,
                                   @Param("customerId") Long customerId);

    /**
     * 제품에 걸린 단가 전부 — 거래처 전용까지 포함한다.
     *
     * <p>{@link #findSeries} 와 헷갈리면 안 된다. 그쪽은 customerId 가 null 이면
     * <b>표준 단가 줄만</b> 돌려주므로, 제품 삭제 정리에 쓰면 거래처 전용 단가가 남아 FK 가 걸린다.
     */
    List<PriceHistory> findAllByProductId(Long productId);

    long countByProductId(Long productId);

    List<PriceHistory> findAllByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);
}
