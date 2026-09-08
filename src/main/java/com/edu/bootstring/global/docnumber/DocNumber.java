package com.edu.bootstring.global.docnumber;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서번호 채번 카운터. 거래처별·연도별로 따로 돈다 (D2-07).
 *
 * <p>거래처가 다르면 다른 행을 잠그므로 락 경합이 오히려 줄어든다.
 * 대신 CUSTOMER_CODE 를 나중에 바꾸면 과거 문서번호와 어긋나므로 등록 후 변경을 막아야 한다.
 */
@Entity
@Getter
@Table(name = "DOC_NUMBER")
@IdClass(DocNumberId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocNumber {

    @Id
    @Column(name = "DOC_TYPE", length = 20)
    private String docType;

    @Id
    @Column(name = "CUSTOMER_CODE", length = 8)
    private String customerCode;

    @Id
    @Column(name = "YEAR_VAL")
    private Integer yearVal;

    @Column(name = "LAST_SEQ", nullable = false)
    private Integer lastSeq;

    public DocNumber(String docType, String customerCode, Integer yearVal) {
        this.docType = docType;
        this.customerCode = customerCode;
        this.yearVal = yearVal;
        this.lastSeq = 0;
    }

    public int increment() {
        return ++this.lastSeq;
    }
}
