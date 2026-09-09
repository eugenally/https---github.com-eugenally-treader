package com.edu.bootstring.statistics;

import java.math.BigDecimal;

/**
 * 제품별 매출 TOP 5
 */
public class ProductSalesDto {

    private String productName;

    private String productCode;

    private BigDecimal totalQty;

    private BigDecimal usdSales;

    private BigDecimal krwSales;

    private Integer rank;

    public ProductSalesDto(String productName, String productCode,
                          BigDecimal totalQty, BigDecimal usdSales,
                          BigDecimal krwSales, Integer rank) {
        this.productName = productName;
        this.productCode = productCode;
        this.totalQty = totalQty;
        this.usdSales = usdSales;
        this.krwSales = krwSales;
        this.rank = rank;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public BigDecimal getTotalQty() {
        return totalQty;
    }

    public BigDecimal getUsdSales() {
        return usdSales;
    }

    public BigDecimal getKrwSales() {
        return krwSales;
    }

    public Integer getRank() {
        return rank;
    }
}
