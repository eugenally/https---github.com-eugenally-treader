package com.edu.bootstring.exchange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일별 환율. 인보이스 발행 시 여기서 읽은 값을 고정한다.
 */
@Entity
@Getter
@Table(name = "EXCHANGE_RATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqExchangeRate")
    @SequenceGenerator(name = "seqExchangeRate", sequenceName = "SEQ_EXCHANGE_RATE", allocationSize = 1)
    @Column(name = "RATE_ID")
    private Long id;

    @Column(name = "BASE_DATE", nullable = false)
    private LocalDate baseDate;

    /** USD, CNH, JPY(100) 같은 통화 단위 */
    @Column(name = "CUR_UNIT", nullable = false, length = 10)
    private String curUnit;

    /** 매매기준율 */
    @Column(name = "DEAL_BAS_R", nullable = false, precision = 15, scale = 4)
    private BigDecimal dealBasR;
}
