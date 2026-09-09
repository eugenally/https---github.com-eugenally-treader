package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.util.List;

/**
 * 통계 조회 계약.
 *
 * <p>Spring Data 리포지토리가 아니다. {@link StatisticsRepositoryImpl} 이 EntityManager 로
 * 직접 구현하므로, 여기에 {@code @Query} 를 달아도 실행되지 않는다. 실제 쿼리는 구현체에만 둔다.
 */
public interface StatisticsRepository {

    /** 월별 매출 (LAG 로 전월 대비). row: [month(DATE), krw_sales, prev_month_sales, yoy_growth] */
    List<Object[]> findMonthlySales();

    /** 거래처별 매출 TOP 5. row: [name_en, customer_code, krw_sales, invoice_count, rank] */
    List<Object[]> findTopCustomers();

    /** 제품별 매출 TOP 5. row: [name_en, product_code, total_qty, usd_sales, krw_sales, rank] */
    List<Object[]> findTopProducts();

    /** 미수금 Aging. row: [bucket, invoice_count, total_krw_amount, outstanding_krw, pct_of_total] */
    List<Object[]> findAgingBuckets();

    /** 총 매출액 (KRW) */
    BigDecimal getTotalSales();

    /** 활성 거래처 수 */
    Long getActiveCustomerCount();

    /** 활성 제품 수 */
    Long getActiveProductCount();

    /** 총 수주 건수 */
    Long getTotalSalesOrderCount();

    /** 총 미수금 (KRW) */
    BigDecimal getOutstandingReceivables();
}
