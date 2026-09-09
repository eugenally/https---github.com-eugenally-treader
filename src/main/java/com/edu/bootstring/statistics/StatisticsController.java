package com.edu.bootstring.statistics;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 통계 API 엔드포인트 (M6 마일스톤)
 */
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "통계 대시보드 API")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 대시보드 전체 요약
     * PIVOT, 윈도우 함수 기반 통계 데이터 한 번에 조회
     */
    @GetMapping("/dashboard-summary")
    @Operation(summary = "대시보드 요약", description = "월별 매출, TOP 거래처/제품, 미수금 Aging 등 통계 전체")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        DashboardSummaryDto summary = statisticsService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * 월별 매출 (YoY 비교)
     */
    @GetMapping("/monthly-sales")
    @Operation(summary = "월별 매출", description = "LAG 윈도우 함수로 전월 대비 성장률 계산")
    public ResponseEntity<List<MonthlySalesDto>> getMonthlySales() {
        List<MonthlySalesDto> sales = statisticsService.getMonthlySales();
        return ResponseEntity.ok(sales);
    }

    /**
     * 거래처별 매출 TOP 5
     */
    @GetMapping("/customer-top")
    @Operation(summary = "거래처별 매출 TOP 5", description = "RANK 윈도우 함수로 순위 지정")
    public ResponseEntity<List<CustomerSalesDto>> getTopCustomers() {
        List<CustomerSalesDto> customers = statisticsService.getTopCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * 제품별 매출 TOP 5
     */
    @GetMapping("/product-top")
    @Operation(summary = "제품별 매출 TOP 5", description = "RANK 윈도우 함수로 순위 지정")
    public ResponseEntity<List<ProductSalesDto>> getTopProducts() {
        List<ProductSalesDto> products = statisticsService.getTopProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 미수금 Aging (결제기한 기준)
     */
    @GetMapping("/receivable-aging")
    @Operation(summary = "미수금 Aging", description = "결제기한 기준 미수금 버킷별 집계")
    public ResponseEntity<List<ReceivableAgingDto>> getAgingBuckets() {
        List<ReceivableAgingDto> aging = statisticsService.getAgingBuckets();
        return ResponseEntity.ok(aging);
    }
}
