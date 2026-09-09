package com.edu.bootstring.exchange;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * M7 마일스톤: 환율 수집 배치 작업
 * 평일 11:30에 한국수출입은행 환율을 수집해 저장한다
 */
@Slf4j
@Component
public class ExchangeRateBatchJob {

    @Autowired
    private ExchangeRateApiClient exchangeRateApiClient;

    /**
     * 평일 11:30 — 환율 수집
     * 한국수출입은행은 영업일 11시 고시
     */
    @Scheduled(cron = "0 30 11 * * MON-FRI", zone = "Asia/Seoul")
    @Transactional
    public void fetchExchangeRates() {
        try {
            log.info("=== 환율 수집 배치 시작 ===");
            LocalDate today = LocalDate.now();

            var rates = exchangeRateApiClient.fetchAndSaveRates(today);

            if (rates.isEmpty()) {
                log.warn("환율 데이터를 받지 못했습니다. 네트워크 상태를 확인하세요.");
            } else {
                log.info("환율 수집 완료: {} 개 통화", rates.size());
                rates.forEach(r ->
                    log.info("  {} {} = {} KRW", r.getBaseDate(), r.getCurUnit(), r.getDealBasR())
                );
            }
        } catch (Exception e) {
            log.error("환율 수집 배치 실패", e);
        }
    }

    /**
     * 매일 00:10 — 유효기한 지난 견적 만료 처리
     * (Quotation.status = EXPIRED 자동 전환)
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void expireQuotations() {
        log.info("견적 만료 배치 시작");
        // TODO: quotationRepository.expireOverdue(LocalDate.now())
        log.info("견적 만료 배치 완료");
    }

    /**
     * 매일 09:00 — 납기 D-7 알림
     * (REQUIRED_DATE - TODAY = 7일인 수주에 알림 생성)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void notifyDeliveryDue() {
        log.info("납기 임박 알림 배치 시작");
        // TODO: alertService.notifyDeliveryDue()
        log.info("납기 임박 알림 배치 완료");
    }

    /**
     * 매일 09:10 — 결제기한 초과 판정
     * (DUE_DATE < TODAY인 인보이스를 OVERDUE로 전환)
     */
    @Scheduled(cron = "0 10 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void markOverdueInvoices() {
        log.info("연체 인보이스 판정 배치 시작");
        // TODO: invoiceRepository.markOverdue(LocalDate.now())
        log.info("연체 인보이스 판정 배치 완료");
    }
}
