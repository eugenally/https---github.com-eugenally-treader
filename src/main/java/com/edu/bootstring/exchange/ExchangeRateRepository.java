package com.edu.bootstring.exchange;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * 기준일 이하에서 가장 최근 환율을 찾는다.
     * 주말·공휴일에는 그날 고시가 없으므로 직전 영업일 값으로 폴백한다.
     */
    List<ExchangeRate> findByCurUnitAndBaseDateLessThanEqualOrderByBaseDateDesc(
            String curUnit, LocalDate baseDate, Limit limit);
}
