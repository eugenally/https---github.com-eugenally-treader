package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 통계 서비스 (M6 마일스톤)
 * PIVOT, 윈도우 함수 기반 네이티브 쿼리 조회 및 DTO 변환
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    @Autowired
    private StatisticsRepository statisticsRepository;

    /**
     * 대시보드 전체 요약 조회
     */
    public DashboardSummaryDto getDashboardSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        // 요약 수치
        summary.setTotalSales(statisticsRepository.getTotalSales());
        summary.setActiveCustomers(statisticsRepository.getActiveCustomerCount());
        summary.setActiveProducts(statisticsRepository.getActiveProductCount());
        summary.setTotalSalesOrders(statisticsRepository.getTotalSalesOrderCount());
        summary.setOutstandingReceivables(statisticsRepository.getOutstandingReceivables());

        // 차트 데이터
        summary.setMonthlySales(getMonthlySales());
        summary.setTopCustomers(getTopCustomers());
        summary.setTopProducts(getTopProducts());
        summary.setAgingBuckets(getAgingBuckets());

        return summary;
    }

    /**
     * 월별 매출 조회
     */
    public List<MonthlySalesDto> getMonthlySales() {
        return statisticsRepository.findMonthlySales().stream()
            .map(row -> new MonthlySalesDto(
                convertToLocalDate(row[0]),  // month
                (BigDecimal) row[1],         // krw_sales
                row[2] != null ? (BigDecimal) row[2] : null,  // prev_month_sales
                row[3] != null ? (BigDecimal) row[3] : null   // yoy_growth
            ))
            .collect(Collectors.toList());
    }

    /**
     * 거래처별 매출 TOP 5
     */
    public List<CustomerSalesDto> getTopCustomers() {
        return statisticsRepository.findTopCustomers().stream()
            .map(row -> new CustomerSalesDto(
                (String) row[0],              // NAME_EN
                (String) row[1],              // CUSTOMER_CODE
                (BigDecimal) row[2],          // krw_sales
                ((Number) row[3]).longValue(), // invoice_count
                ((Number) row[4]).intValue()   // rank
            ))
            .collect(Collectors.toList());
    }

    /**
     * 제품별 매출 TOP 5
     */
    public List<ProductSalesDto> getTopProducts() {
        return statisticsRepository.findTopProducts().stream()
            .map(row -> new ProductSalesDto(
                (String) row[0],                           // NAME_EN
                (String) row[1],                           // PRODUCT_CODE
                (BigDecimal) row[2],                       // total_qty
                (BigDecimal) row[3],                       // usd_sales
                (BigDecimal) row[4],                       // krw_sales
                ((Number) row[5]).intValue()               // rank
            ))
            .collect(Collectors.toList());
    }

    /**
     * 미수금 Aging 버킷별 집계
     */
    public List<ReceivableAgingDto> getAgingBuckets() {
        return statisticsRepository.findAgingBuckets().stream()
            .map(row -> new ReceivableAgingDto(
                (String) row[0],                           // aging_bucket
                ((Number) row[1]).longValue(),             // invoice_count
                (BigDecimal) row[2],                       // total_krw_amount
                (BigDecimal) row[3],                       // outstanding_krw
                row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO  // pct_of_total
            ))
            .collect(Collectors.toList());
    }

    /**
     * Object를 LocalDate로 변환
     */
    private LocalDate convertToLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDate) return (LocalDate) obj;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) obj).getTime()).toLocalDate();
        }
        return null;
    }
}
