package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Repository
public class StatisticsRepositoryImpl implements StatisticsRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Object[]> findMonthlySales() {
        String sql = """
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
                RANK() OVER (ORDER BY SUM(i.AMOUNT * i.EXCHANGE_RATE) DESC) AS rank
            FROM INVOICE i
            JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
            JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            GROUP BY c.CUSTOMER_ID, c.NAME_EN, c.CUSTOMER_CODE
            ORDER BY rank
            FETCH FIRST 5 ROWS ONLY
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
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public List<Object[]> findAgingBuckets() {
        String sql = """
            SELECT
                CASE
                    WHEN i.AMOUNT_PAID = i.AMOUNT THEN '완납'
                    WHEN TRUNC(SYSDATE) < i.DUE_DATE THEN '미만기'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 30 THEN '30일'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 60 THEN '60일'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 90 THEN '90일'
                    ELSE '90일초과'
                END AS aging_bucket,
                COUNT(*) AS invoice_count,
                SUM(i.AMOUNT - i.AMOUNT_PAID) AS outstanding_amount,
                ROUND(RATIO_TO_REPORT(SUM(i.AMOUNT - i.AMOUNT_PAID)) OVER () * 100, 2) AS percentage
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS != 'VOID'
            GROUP BY
                CASE
                    WHEN i.AMOUNT_PAID = i.AMOUNT THEN '완납'
                    WHEN TRUNC(SYSDATE) < i.DUE_DATE THEN '미만기'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 30 THEN '30일'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 60 THEN '60일'
                    WHEN TRUNC(SYSDATE) - i.DUE_DATE < 90 THEN '90일'
                    ELSE '90일초과'
                END
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public BigDecimal getTotalSales() {
        String sql = """
            SELECT SUM(i.AMOUNT * i.EXCHANGE_RATE)
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
            """;
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }

    @Override
    public Long getActiveCustomerCount() {
        String sql = """
            SELECT COUNT(DISTINCT c.CUSTOMER_ID)
            FROM CUSTOMER c
            WHERE c.ACTIVE_YN = 'Y'
            """;
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public Long getActiveProductCount() {
        String sql = """
            SELECT COUNT(DISTINCT p.PRODUCT_ID)
            FROM PRODUCT p
            WHERE p.ACTIVE_YN = 'Y'
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
        String sql = """
            SELECT COALESCE(SUM(i.AMOUNT - i.AMOUNT_PAID), 0)
            FROM INVOICE i
            WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'ISSUED' AND i.AMOUNT > i.AMOUNT_PAID
            """;
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }
}
