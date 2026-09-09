package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;

/**
 * 통계 네이티브 쿼리.
 *
 * <p>Oracle 의 TRUNC(date,'MM') 과 RATIO_TO_REPORT 는 쓰지 않는다. TRUNC 는 MariaDB 12 의
 * Oracle 호환 함수로 우연히 동작하지만 RDS 가 흔히 쓰는 10.6 에는 없어서, 로컬에서만 되고
 * 배포하면 깨진다. 비율은 GROUP BY 뒤에 도는 SUM(SUM(x)) OVER () 로 대신한다.
 */
@Repository
public class StatisticsRepositoryImpl implements StatisticsRepository {

    /** 미수 잔액(원화). 여러 쿼리가 같은 식을 써서 한 곳에 둔다. */
    private static final String OUTSTANDING_KRW =
            "(i.AMOUNT - COALESCE(i.PAID_AMOUNT, 0)) * i.EXCHANGE_RATE";

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Object[]> findMonthlySales() {
        // month 는 DATE 로 내보낸다. StatisticsService.convertToLocalDate 가 문자열은 못 받는다.
        String sql = """
            SELECT
                CAST(DATE_FORMAT(sh.ETD, '%Y-%m-01') AS DATE) AS month,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
                LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE))
                    OVER (ORDER BY DATE_FORMAT(sh.ETD, '%Y-%m-01')) AS prev_month_sales,
                ROUND(((SUM(i.AMOUNT * i.EXCHANGE_RATE) / NULLIF(
                        LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE))
                            OVER (ORDER BY DATE_FORMAT(sh.ETD, '%Y-%m-01')), 0)) - 1) * 100, 2) AS yoy_growth
            FROM INVOICE i
            JOIN SHIPMENT sh ON i.SHIPMENT_ID = sh.SHIPMENT_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY DATE_FORMAT(sh.ETD, '%Y-%m-01')
            ORDER BY month DESC
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public List<Object[]> findTopCustomers() {
        String sql = """
            SELECT
                c.NAME_EN,
                c.CUSTOMER_CODE,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
                COUNT(DISTINCT i.INVOICE_ID) AS invoice_count,
                RANK() OVER (ORDER BY SUM(i.AMOUNT * i.EXCHANGE_RATE) DESC) AS `rank`
            FROM INVOICE i
            JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
            JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY c.CUSTOMER_ID, c.NAME_EN, c.CUSTOMER_CODE
            ORDER BY `rank`
            LIMIT 5
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public List<Object[]> findTopProducts() {
        String sql = """
            SELECT
                p.NAME_EN,
                p.PRODUCT_CODE,
                SUM(si.QTY) AS total_qty,
                SUM(si.QTY * soi.UNIT_PRICE) AS usd_sales,
                SUM(si.QTY * soi.UNIT_PRICE * i.EXCHANGE_RATE) AS krw_sales,
                RANK() OVER (ORDER BY SUM(si.QTY * soi.UNIT_PRICE) DESC) AS `rank`
            FROM SHIPMENT_ITEM si
            JOIN PRODUCT p ON si.PRODUCT_ID = p.PRODUCT_ID
            JOIN SALES_ORDER_ITEM soi ON si.ORDER_ITEM_ID = soi.ITEM_ID
            JOIN SALES_ORDER so ON soi.ORDER_ID = so.ORDER_ID
            JOIN INVOICE i ON so.ORDER_ID = i.ORDER_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY p.PRODUCT_ID, p.NAME_EN, p.PRODUCT_CODE
            ORDER BY `rank`
            LIMIT 5
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public List<Object[]> findAgingBuckets() {
        String sql = """
            SELECT
                CASE
                    WHEN i.STATUS = 'PAID'                        THEN '완납'
                    WHEN CURDATE() <= i.DUE_DATE                  THEN '미만기'
                    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) <= 30    THEN '30일'
                    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) <= 60    THEN '60일'
                    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) <= 90    THEN '90일'
                    ELSE '90일 초과'
                END AS aging_bucket,
                COUNT(*) AS invoice_count,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS total_krw_amount,
                SUM(%1$s) AS outstanding_krw,
                ROUND(SUM(%1$s) / NULLIF(SUM(SUM(%1$s)) OVER (), 0) * 100, 2) AS pct_of_total
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS <> 'CANCELLED'
            GROUP BY aging_bucket
            ORDER BY FIELD(aging_bucket, '완납', '미만기', '30일', '60일', '90일', '90일 초과')
            """.formatted(OUTSTANDING_KRW);
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public BigDecimal getTotalSales() {
        String sql = """
            SELECT COALESCE(SUM(i.AMOUNT * i.EXCHANGE_RATE), 0)
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            """;
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }

    @Override
    public Long getActiveCustomerCount() {
        String sql = """
            SELECT COUNT(*)
            FROM CUSTOMER c
            WHERE c.STATUS = 'ACTIVE'
            """;
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public Long getActiveProductCount() {
        String sql = """
            SELECT COUNT(*)
            FROM PRODUCT p
            WHERE p.STATUS = 'ACTIVE'
            """;
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public Long getTotalSalesOrderCount() {
        String sql = """
            SELECT COUNT(*)
            FROM SALES_ORDER
            """;
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public BigDecimal getOutstandingReceivables() {
        // PARTIALLY_PAID·OVERDUE 도 미수다. ISSUED 만 세면 부분입금 건이 빠진다.
        String sql = """
            SELECT COALESCE(SUM(%s), 0)
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS NOT IN ('PAID', 'CANCELLED')
            """.formatted(OUTSTANDING_KRW);
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }
}
