package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.util.List;

/**
 * 대시보드 전체 요약
 */
public class DashboardSummaryDto {

    // 요약 수치
    private BigDecimal totalSales;       // 총 매출액 (KRW)
    private Long activeCustomers;        // 활성 거래처 수
    private Long activeProducts;         // 활성 제품 수
    private Long totalSalesOrders;       // 총 수주 건수
    private BigDecimal outstandingReceivables;  // 미수금 (KRW)

    // 차트 데이터
    private List<MonthlySalesDto> monthlySales;
    private List<CustomerSalesDto> topCustomers;
    private List<ProductSalesDto> topProducts;
    private List<ReceivableAgingDto> agingBuckets;

    public DashboardSummaryDto() {
    }

    // Getters & Setters
    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Long getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(Long activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public Long getActiveProducts() {
        return activeProducts;
    }

    public void setActiveProducts(Long activeProducts) {
        this.activeProducts = activeProducts;
    }

    public Long getTotalSalesOrders() {
        return totalSalesOrders;
    }

    public void setTotalSalesOrders(Long totalSalesOrders) {
        this.totalSalesOrders = totalSalesOrders;
    }

    public BigDecimal getOutstandingReceivables() {
        return outstandingReceivables;
    }

    public void setOutstandingReceivables(BigDecimal outstandingReceivables) {
        this.outstandingReceivables = outstandingReceivables;
    }

    public List<MonthlySalesDto> getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(List<MonthlySalesDto> monthlySales) {
        this.monthlySales = monthlySales;
    }

    public List<CustomerSalesDto> getTopCustomers() {
        return topCustomers;
    }

    public void setTopCustomers(List<CustomerSalesDto> topCustomers) {
        this.topCustomers = topCustomers;
    }

    public List<ProductSalesDto> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<ProductSalesDto> topProducts) {
        this.topProducts = topProducts;
    }

    public List<ReceivableAgingDto> getAgingBuckets() {
        return agingBuckets;
    }

    public void setAgingBuckets(List<ReceivableAgingDto> agingBuckets) {
        this.agingBuckets = agingBuckets;
    }
}
