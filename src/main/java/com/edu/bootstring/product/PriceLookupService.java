package com.edu.bootstring.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 적용 단가 조회. 거래처 전용 단가가 있으면 그것을, 없으면 표준 단가를 쓴다.
 */
@Service
@RequiredArgsConstructor
public class PriceLookupService {

    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional(readOnly = true)
    public Optional<PriceHistory> findApplicable(Long productId, Long customerId, LocalDate baseDate) {
        List<PriceHistory> candidates =
                priceHistoryRepository.findApplicable(productId, customerId, baseDate);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }
}
