-- ===============================================
-- M6 통계 쿼리 모음
-- 프로젝트 내 DashboardPage 및 리포트에서 사용
-- ===============================================

-- 1. 월별 매출 (YoY 비교) - LAG 윈도우함수
-- 라인 차트용: 월별 추이 + 년간 비교율
SELECT
  DATE_FORMAT(sh.ETD, '%Y-%m-01') AS month,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
  LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE)) OVER (ORDER BY DATE_FORMAT(sh.ETD, '%Y-%m-01')) AS prev_month_sales,
  ROUND(((SUM(i.AMOUNT * i.EXCHANGE_RATE) /
          LAG(SUM(i.AMOUNT * i.EXCHANGE_RATE)) OVER (ORDER BY DATE_FORMAT(sh.ETD, '%Y-%m-01'))) - 1) * 100, 2) AS yoy_growth_rate
FROM INVOICE i
JOIN SHIPMENT sh ON i.SHIPMENT_ID = sh.SHIPMENT_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY DATE_FORMAT(sh.ETD, '%Y-%m-01')
ORDER BY month DESC;


-- 2. 거래처별 매출 TOP 5 - RANK 윈도우함수
-- 테이블/바 차트용: 거래처별 순위 및 매출액
SELECT
  c.NAME_EN AS customer_name,
  c.CUSTOMER_CODE AS customer_code,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales,
  COUNT(DISTINCT i.INVOICE_ID) AS invoice_count,
  RANK() OVER (ORDER BY SUM(i.AMOUNT * i.EXCHANGE_RATE) DESC) AS sales_rank
FROM INVOICE i
JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY c.CUSTOMER_ID, c.NAME_EN, c.CUSTOMER_CODE
ORDER BY sales_rank
LIMIT 5;


-- 3. 제품별 매출 TOP 5 - RANK 윈도우함수
-- 테이블/바 차트용: 제품별 순위, 수량, USD/KRW 매출
SELECT
  p.NAME_EN AS product_name,
  p.PRODUCT_CODE AS product_code,
  SUM(si.QTY) AS total_qty,
  SUM(si.QTY * soi.UNIT_PRICE) AS usd_sales,
  SUM(si.QTY * soi.UNIT_PRICE * i.EXCHANGE_RATE) AS krw_sales,
  RANK() OVER (ORDER BY SUM(si.QTY * soi.UNIT_PRICE) DESC) AS sales_rank
FROM SHIPMENT_ITEM si
JOIN PRODUCT p ON si.PRODUCT_ID = p.PRODUCT_ID
JOIN SALES_ORDER_ITEM soi ON si.ORDER_ITEM_ID = soi.ITEM_ID
JOIN SALES_ORDER so ON soi.ORDER_ID = so.ORDER_ID
JOIN INVOICE i ON so.ORDER_ID = i.ORDER_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY p.PRODUCT_ID, p.NAME_EN, p.PRODUCT_CODE
ORDER BY sales_rank
LIMIT 5;


-- 4. 미수금 Aging 분석 - RATIO_TO_REPORT 윈도우함수
-- 파이 차트용: 연령대별 미수금 비율 및 건수
SELECT
  CASE
    WHEN i.AMOUNT_PAID = i.AMOUNT THEN '완납'
    WHEN CURDATE() < i.DUE_DATE THEN '미만기'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 30 THEN '30일'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 60 THEN '60일'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 90 THEN '90일'
    ELSE '90일초과'
  END AS aging_bucket,
  COUNT(*) AS invoice_count,
  SUM(i.AMOUNT - i.AMOUNT_PAID) AS outstanding_amount,
  ROUND(RATIO_TO_REPORT(SUM(i.AMOUNT - i.AMOUNT_PAID)) OVER () * 100, 2) AS aging_percentage
FROM INVOICE i
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS != 'VOID'
GROUP BY
  CASE
    WHEN i.AMOUNT_PAID = i.AMOUNT THEN '완납'
    WHEN CURDATE() < i.DUE_DATE THEN '미만기'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 30 THEN '30일'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 60 THEN '60일'
    WHEN DATEDIFF(CURDATE(), i.DUE_DATE) < 90 THEN '90일'
    ELSE '90일초과'
  END
ORDER BY aging_percentage DESC;


-- 5. 핵심 지표 (KPI) - 스칼라 쿼리들

-- 5-1. 총 매출액 (원화, PAID 기준)
SELECT SUM(i.AMOUNT * i.EXCHANGE_RATE) AS total_krw_sales
FROM INVOICE i
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID';


-- 5-2. 활성 거래처 수
SELECT COUNT(DISTINCT c.CUSTOMER_ID) AS active_customer_count
FROM CUSTOMER c
WHERE c.STATUS = 'ACTIVE';


-- 5-3. 활성 제품 수
SELECT COUNT(DISTINCT p.PRODUCT_ID) AS active_product_count
FROM PRODUCT p
WHERE p.STATUS = 'ACTIVE';


-- 5-4. 총 수주 수
SELECT COUNT(*) AS total_sales_order_count
FROM SALES_ORDER;


-- 5-5. 미결제 미수금 합계
SELECT COALESCE(SUM(i.AMOUNT - i.AMOUNT_PAID), 0) AS outstanding_receivables
FROM INVOICE i
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE');


-- ===============================================
-- 추가 분석 쿼리
-- ===============================================

-- 6. 월별 수주 건수 (트렌드)
SELECT
  DATE_FORMAT(so.ORDER_DATE, '%Y-%m-01') AS order_month,
  COUNT(*) AS order_count,
  COUNT(DISTINCT so.CUSTOMER_ID) AS unique_customers
FROM SALES_ORDER so
GROUP BY DATE_FORMAT(so.ORDER_DATE, '%Y-%m-01')
ORDER BY order_month DESC;


-- 7. 거래처별 평균 주문액
SELECT
  c.NAME_EN,
  AVG(i.AMOUNT * i.EXCHANGE_RATE) AS avg_order_krw,
  COUNT(*) AS order_count
FROM INVOICE i
JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY c.CUSTOMER_ID, c.NAME_EN
ORDER BY avg_order_krw DESC;


-- 8. 출하 대기 항목 (배송 예정)
SELECT
  so.ORDER_NO,
  c.NAME_EN AS customer,
  sh.ETD,
  sh.SHIPMENT_NO,
  DATEDIFF(sh.ETD, CURDATE()) AS days_to_shipment
FROM SALES_ORDER so
JOIN SHIPMENT sh ON so.ORDER_ID = sh.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE sh.STATUS != 'SHIPPED' AND sh.ETD IS NOT NULL
ORDER BY sh.ETD ASC;


-- 9. 결제 기한 임박 인보이스 (D-7)
SELECT
  i.INVOICE_NO,
  c.NAME_EN,
  i.AMOUNT * i.EXCHANGE_RATE AS krw_amount,
  i.DUE_DATE,
  DATEDIFF(i.DUE_DATE, CURDATE()) AS days_to_due
FROM INVOICE i
JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE i.STATUS IN ('ISSUED', 'PARTIALLY_PAID')
  AND DATEDIFF(i.DUE_DATE, CURDATE()) BETWEEN 0 AND 7
ORDER BY i.DUE_DATE ASC;


-- 10. 연체 인보이스
SELECT
  i.INVOICE_NO,
  c.NAME_EN,
  i.AMOUNT * i.EXCHANGE_RATE AS krw_amount,
  i.DUE_DATE,
  DATEDIFF(CURDATE(), i.DUE_DATE) AS overdue_days
FROM INVOICE i
JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE i.STATUS = 'OVERDUE'
ORDER BY overdue_days DESC;

-- ===============================================
-- 실행 예시
-- ===============================================
/*

-- 월별 매출 라인 차트 데이터 조회
SELECT
  DATE_FORMAT(sh.ETD, '%Y-%m-01') AS month,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales
FROM INVOICE i
JOIN SHIPMENT sh ON i.SHIPMENT_ID = sh.SHIPMENT_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY DATE_FORMAT(sh.ETD, '%Y-%m-01')
ORDER BY month DESC
LIMIT 12;

-- 거래처 TOP 5 바 차트
-- MariaDB: DBMS_OUTPUT 대신 SELECT로 표시
SELECT
  ROW_NUMBER() OVER (ORDER BY SUM(i.AMOUNT * i.EXCHANGE_RATE) DESC) AS rank,
  c.NAME_EN,
  SUM(i.AMOUNT * i.EXCHANGE_RATE) AS krw_sales
FROM INVOICE i
JOIN SALES_ORDER so ON i.ORDER_ID = so.ORDER_ID
JOIN CUSTOMER c ON so.CUSTOMER_ID = c.CUSTOMER_ID
WHERE i.INVOICE_TYPE = 'CI' AND i.STATUS = 'PAID'
GROUP BY c.CUSTOMER_ID, c.NAME_EN
LIMIT 5;

*/
