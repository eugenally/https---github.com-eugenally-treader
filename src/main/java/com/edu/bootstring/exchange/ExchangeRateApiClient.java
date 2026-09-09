package com.edu.bootstring.exchange;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국수출입은행 환율 API 클라이언트
 * https://www.koreaexim.go.kr/ir/api/searchExchangeRateList
 */
@Slf4j
@Component
public class ExchangeRateApiClient {

    private static final String KOREAEXIM_API_URL = "https://www.koreaexim.go.kr/ir/api/searchExchangeRateList";
    private static final String API_KEY = "1101001000000000001";  // 공개 API 키

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * 기준일의 환율 데이터 조회 및 저장
     */
    public List<ExchangeRate> fetchAndSaveRates(LocalDate baseDate) {
        try {
            log.info("환율 데이터 조회 시작: {}", baseDate);

            String formattedDate = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = String.format("%s?authkey=%s&searchdate=%s&data=AP01", KOREAEXIM_API_URL, API_KEY, formattedDate);

            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseResponse(response, baseDate);

            if (!rates.isEmpty()) {
                exchangeRateRepository.saveAll(rates);
                log.info("환율 데이터 저장 완료: {} 건", rates.size());
            }

            return rates;
        } catch (Exception e) {
            log.error("환율 API 호출 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * API 응답 파싱
     */
    private List<ExchangeRate> parseResponse(String response, LocalDate baseDate) {
        List<ExchangeRate> rates = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.isArray()) {
                root.forEach(node -> {
                    String curUnit = node.get("cur_unit").asText();
                    String dealBasRStr = node.get("deal_bas_r").asText().replace(",", "");
                    BigDecimal dealBasR = new BigDecimal(dealBasRStr);

                    ExchangeRate rate = new ExchangeRate(baseDate, curUnit, dealBasR);
                    rates.add(rate);
                });
            }
        } catch (Exception e) {
            log.error("환율 데이터 파싱 실패: {}", e.getMessage(), e);
        }

        return rates;
    }

    /**
     * 지정 통화의 최근 환율 조회 (주말·공휴일 폴백)
     */
    public BigDecimal getLatestRate(String curUnit, LocalDate date) {
        List<ExchangeRate> rates = exchangeRateRepository
            .findByCurUnitAndBaseDateLessThanEqualOrderByBaseDateDesc(
                curUnit, date, org.springframework.data.domain.Limit.of(1));

        if (!rates.isEmpty()) {
            return rates.get(0).getDealBasR();
        }

        log.warn("환율 정보 없음: {} as of {}", curUnit, date);
        return BigDecimal.ZERO;
    }
}
