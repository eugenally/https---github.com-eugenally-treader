package com.edu.bootstring.global.docnumber;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * 문서번호 채번. {@code QT-ABC-2026-0001} 형식이다.
 */
@Service
@RequiredArgsConstructor
public class DocNumberService {

    private final DocNumberRepository docNumberRepository;

    /**
     * 다음 번호를 뽑는다.
     *
     * <p>{@code REQUIRES_NEW} 로 별도 트랜잭션에서 즉시 커밋해 락 보유 시간을 최소화한다.
     * 바깥 트랜잭션이 롤백되면 번호에 구멍이 생기지만, 이 프로젝트는 그걸 감수한다.
     * 세금계산서처럼 번호 연속성이 법적 요건인 경우와는 다르다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String docType, String customerCode) {
        int year = Year.now().getValue();

        DocNumber counter = docNumberRepository.findForUpdate(docType, customerCode, year)
                .orElseGet(() -> docNumberRepository.saveAndFlush(
                        new DocNumber(docType, customerCode, year)));

        int seq = counter.increment();
        docNumberRepository.saveAndFlush(counter);

        return "%s-%s-%d-%04d".formatted(docType, customerCode, year, seq);
    }
}
