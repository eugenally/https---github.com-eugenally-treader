package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StatisticsRepository {

    /**
     * 월별 매출 (YoY 비교) - LAG 윈도우 함수
     */
    @Query(value = """
            SELECT
                TRUNC(sh.ETD, 'MM') AS month,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
                LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE)) OVER (ORDER BY TRUNC(sh.ETD, 'MM')) AS prev_month_sales,
                ROUND(((SUM(i.AMOUNT * i.EXCHANGE_RATE) /
                        LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE)) OVER (ORDER BY TRUNC(sh.ETD, 'MM'))) - 1) * 100, 2) AS yoy_growth
            FROM INVOICE i
            JOIN SHIPMENT sh ON i.SHIPMENT_ID = sh.SHIPMENT_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY TRUNC(sh.ETD, 'MM')
            ORDER BY month DESC
            """, nativeQuery = true)
    List<Object[]> findMonthlySales();

    /**
     * 거래처별 매출 TOP 5
     */
    @Query(value = """
            SELECT
                c.NAME_EN,
                c.CUSTOMER_CODE,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
                COUNT(DISTINCT i.INVOICE_ID) AS invoice_count,
                RANK() OVER (ORDER BY SUM(i.AMOUNT * i.EXCHANGE_RATE) DESC) AS rank
            FROM INVOICE i
            JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
            JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY c.CUSTOMER_ID, c.NAME_EN, c.CUSTOMER_CODE
            ORDER BY rank
            FETCH FIRST 5 ROWS ONLY
            """, nativeQuery = true)
    List<Object[]> findTopCustomers();

    /**
     * 제품별 매출 TOP 5
     */
    @Query(value = """
            SELECT
                p.NAME_EN,
                p.PRODUCT_CODE,
                SUM(si.QTY) AS total_qty,
                SUM(si.QTY * soi.UNIT_PRICE) AS usd_sales,
                SUM(si.QTY * soi.UNIT_PRICE * i.EXCHANGE_RATE) AS krw_sales,
                RANK() OVER (ORDER BY SUM(si.QTY * soi.UNIT_PRICE) DESC) AS rank
            FROM SHIPMENT_ITEM si
            JOIN PRODUCT p ON si.PRODUCT_ID = p.PRODUCT_ID
            JOIN SALES_ORDER_ITEM soi ON si.ORDER_ITEM_ID = soi.ITEM_ID
            JOIN SALES_ORDER so ON soi.ORDER_ID = so.ORDER_ID
            JOIN INVOICE i ON so.ORDER_ID = i.ORDER_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY p.PRODUCT_ID, p.NAME_EN, p.PRODUCT_CODE
            ORDER BY rank
            FETCH FIRST 5 ROWS ONLY
            """, nativeQuery = true)
    List<Object[]> findTopProducts();

    /**
     * 미수금 Aging Summary (버킷별 집계)
     */
    @Query(value = """
            SELECT
                CASE
                    WHEN i.STATUS = 'PAID' THEN '완납'
                    WHEN TRUNC(SYSDATE) <= i.DUE_DATE THEN '미만기'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 1 AND 30 THEN '30일 이상'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 31 AND 60 THEN '60일 이상'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 61 AND 90 THEN '90일 이상'
                    ELSE '90일 초과'
                END AS aging_bucket,
                COUNT(*) AS invoice_count,
                SUM(i.AMOUNT * i.EXCHANGE_RATE) AS total_krw_amount,
                SUM((i.AMOUNT * i.EXCHANGE_RATE) - COALESCE(i.PAID_AMOUNT * i.EXCHANGE_RATE, 0)) AS outstanding_krw,
                RATIO_TO_REPORT(SUM((i.AMOUNT * i.EXCHANGE_RATE) - COALESCE(i.PAID_AMOUNT * i.EXCHANGE_RATE, 0)))
                    OVER () * 100 AS pct_of_total
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI'
            GROUP BY
                CASE
                    WHEN i.STATUS = 'PAID' THEN '완납'
                    WHEN TRUNC(SYSDATE) <= i.DUE_DATE THEN '미만기'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 1 AND 30 THEN '30일 이상'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 31 AND 60 THEN '60일 이상'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE BETWEEN 61 AND 90 THEN '90일 이상'
                    ELSE '90일 초과'
                END
            ORDER BY
                CASE aging_bucket
                    WHEN '완납' THEN 0
                    WHEN '미만기' THEN 1
                    WHEN '30일 이상' THEN 2
                    WHEN '60일 이상' THEN 3
                    WHEN '90일 이상' THEN 4
                    ELSE 5
                END
            """, nativeQuery = true)
    List<Object[]> findAgingBuckets();

    /**
     * 총 매출액 (KRW)
     */
    @Query(value = """
            SELECT SUM(i.AMOUNT * i.EXCHANGE_RATE)
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            """, nativeQuery = true)
    BigDecimal getTotalSales();

    /**
     * 활성 거래처 수
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM CUSTOMER c
            WHERE c.STATUS = 'ACTIVE'
            """, nativeQuery = true)
    Long getActiveCustomerCount();

    /**
     * 활성 제품 수
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM PRODUCT p
            WHERE p.STATUS = 'ACTIVE'
            """, nativeQuery = true)
    Long getActiveProductCount();

    /**
     * 총 수주 건수
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM SALES_ORDER
            """, nativeQuery = true)
    Long getTotalSalesOrderCount();

    /**
     * 총 미수금 (KRW)
     */
    @Query(value = """
            SELECT SUM((i.AMOUNT * i.EXCHANGE_RATE) - COALESCE(i.PAID_AMOUNT * i.EXCHANGE_RATE, 0))
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS <> 'PAID'
            """, nativeQuery = true)
    BigDecimal getOutstandingReceivables();
}
