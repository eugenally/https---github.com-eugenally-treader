package com.edu.bootstring.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 월별 매출 통계 (YoY 비교)
 */
public class MonthlySalesDto {

    @JsonFormat(pattern = "yyyy-MM")
    private LocalDate month;

    private BigDecimal krwSales;

    private BigDecimal prevMonthSales;

    private BigDecimal yoyGrowthRate;

    public MonthlySalesDto(LocalDate month, BigDecimal krwSales,
                          BigDecimal prevMonthSales, BigDecimal yoyGrowthRate) {
        this.month = month;
        this.krwSales = krwSales;
        this.prevMonthSales = prevMonthSales;
        this.yoyGrowthRate = yoyGrowthRate;
    }

    // Getters
    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getKrwSales() {
        return krwSales;
    }

    public BigDecimal getPrevMonthSales() {
        return prevMonthSales;
    }

    public BigDecimal getYoyGrowthRate() {
        return yoyGrowthRate;
    }
}
