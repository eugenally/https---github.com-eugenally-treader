package com.edu.bootstring.global.docnumber;

import java.io.Serializable;
import java.util.Objects;

/**
 * DOC_NUMBER 복합키
 */
public class DocNumberId implements Serializable {

    private String docType;
    private String customerCode;
    private Integer yearVal;

    public DocNumberId() {
    }

    public DocNumberId(String docType, String customerCode, Integer yearVal) {
        this.docType = docType;
        this.customerCode = customerCode;
        this.yearVal = yearVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocNumberId other)) {
            return false;
        }
        return Objects.equals(docType, other.docType)
                && Objects.equals(customerCode, other.customerCode)
                && Objects.equals(yearVal, other.yearVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docType, customerCode, yearVal);
    }
}
