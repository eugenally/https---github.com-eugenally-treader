package com.edu.bootstring.statistics;

import java.math.BigDecimal;

/**
 * 미수금 Aging (결제기한 기준)
 */
public class ReceivableAgingDto {

    private String agingBucket;  // 완납, 미만기, 30일 이상, 60일 이상, 90일 이상, 90일 초과

    private Long invoiceCount;

    private BigDecimal totalKrwAmount;

    private BigDecimal outstandingKrw;

    private BigDecimal percentOfTotal;  // 전체 미수금 중 비율

    public ReceivableAgingDto(String agingBucket, Long invoiceCount,
                             BigDecimal totalKrwAmount, BigDecimal outstandingKrw,
                             BigDecimal percentOfTotal) {
        this.agingBucket = agingBucket;
        this.invoiceCount = invoiceCount;
        this.totalKrwAmount = totalKrwAmount;
        this.outstandingKrw = outstandingKrw;
        this.percentOfTotal = percentOfTotal;
    }

    // Getters
    public String getAgingBucket() {
        return agingBucket;
    }

    public Long getInvoiceCount() {
        return invoiceCount;
    }

    public BigDecimal getTotalKrwAmount() {
        return totalKrwAmount;
    }

    public BigDecimal getOutstandingKrw() {
        return outstandingKrw;
    }

    public BigDecimal getPercentOfTotal() {
        return percentOfTotal;
    }
}
