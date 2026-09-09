package com.edu.bootstring.statistics;

import java.math.BigDecimal;

/**
 * 거래처별 매출 TOP 5
 */
public class CustomerSalesDto {

    private String customerName;

    private String customerCode;

    private BigDecimal krwSales;

    private Long invoiceCount;

    private Integer rank;

    public CustomerSalesDto(String customerName, String customerCode,
                           BigDecimal krwSales, Long invoiceCount, Integer rank) {
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.krwSales = krwSales;
        this.invoiceCount = invoiceCount;
        this.rank = rank;
    }

    // Getters
    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public BigDecimal getKrwSales() {
        return krwSales;
    }

    public Long getInvoiceCount() {
        return invoiceCount;
    }

    public Integer getRank() {
        return rank;
    }
}
