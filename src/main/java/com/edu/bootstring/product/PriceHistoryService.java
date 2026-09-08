package com.edu.bootstring.product;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.product.dto.ProductDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 단가 이력 관리.
 *
 * <p>(거래처 × 제품 × 기간) 조합이라 기간이 겹치면 "이 날짜에 어떤 단가를 쓸지"가 모호해진다.
 * 범위 겹침은 UNIQUE 제약으로 표현할 수 없어 <b>DB 가 막아 주지 못한다.</b> 그래서 여기서 책임진다.
 */
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<ProductDtos.PriceResponse> findList(Long productId, Long customerId) {
        List<PriceHistory> prices = priceHistoryRepository.findForList(productId, customerId);
        return toResponses(prices);
    }

    /**
     * 단가 등록.
     *
     * <p>같은 (거래처, 제품) 줄에서 기간이 겹치면 두 갈래로 처리한다.
     * <ul>
     *   <li>{@code closePrevious=true} — 겹치는 이전 단가를 새 시작일 <b>하루 전</b>으로 닫는다.
     *       가격 인상처럼 "오늘부터 새 단가" 인 실무 상황이 여기에 해당한다.</li>
     *   <li>{@code closePrevious=false} — 오류로 돌려준다. 과거 구간을 소급 입력하다가
     *       기존 이력을 조용히 덮는 사고를 막는다.</li>
     * </ul>
     */
    @Transactional
    public ProductDtos.PriceResponse create(ProductDtos.PriceCreateRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));

        if (req.customerId() != null && !customerRepository.existsById(req.customerId())) {
            throw new NotFoundException("거래처를 찾을 수 없습니다.");
        }
        if (req.validTo() != null && req.validTo().isBefore(req.validFrom())) {
            throw new BusinessException("종료일이 시작일보다 빠릅니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        List<PriceHistory> series = priceHistoryRepository.findSeries(req.productId(), req.customerId());
        List<PriceHistory> overlapping = series.stream()
                .filter(p -> p.overlaps(req.validFrom(), req.validTo()))
                .toList();

        if (!overlapping.isEmpty()) {
            if (!req.shouldClosePrevious()) {
                PriceHistory first = overlapping.get(0);
                throw new BusinessException(
                        "적용 기간이 기존 단가(%s ~ %s, %s)와 겹칩니다. 이전 단가를 자동으로 마감하려면 '이전 단가 마감' 을 선택하세요."
                                .formatted(first.getValidFrom(),
                                        first.getValidTo() != null ? first.getValidTo() : "무기한",
                                        first.getUnitPrice()),
                        ErrorCode.INVALID_INPUT_VALUE);
            }
            // 새 단가보다 앞서 시작한 건만 닫을 수 있다.
            // 새 단가 이후에 시작하는 미래 단가를 뒤로 밀 수는 없으므로 그건 거절한다.
            List<PriceHistory> future = overlapping.stream()
                    .filter(p -> !p.getValidFrom().isBefore(req.validFrom()))
                    .toList();
            if (!future.isEmpty()) {
                throw new BusinessException(
                        "%s 부터 시작하는 단가가 이미 있습니다. 그 단가를 먼저 정리해 주세요."
                                .formatted(future.get(0).getValidFrom()),
                        ErrorCode.INVALID_INPUT_VALUE);
            }
            overlapping.forEach(p -> p.closeBefore(req.validFrom()));
        }

        PriceHistory saved = priceHistoryRepository.save(PriceHistory.builder()
                .productId(req.productId())
                .customerId(req.customerId())
                .currency(req.currency())
                .unitPrice(req.unitPrice())
                .incoterms(req.incoterms())
                .minQty(req.minQty())
                .validFrom(req.validFrom())
                .validTo(req.validTo())
                .remark(req.remark())
                .build());

        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public ProductDtos.PriceResponse update(Long id, ProductDtos.PriceUpdateRequest req) {
        PriceHistory price = get(id);

        if (req.validTo() != null && req.validTo().isBefore(price.getValidFrom())) {
            throw new BusinessException("종료일이 시작일보다 빠릅니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        // 종료일을 늘리면 뒤 단가와 겹칠 수 있다
        if (req.validTo() != null) {
            boolean collides = priceHistoryRepository
                    .findSeries(price.getProductId(), price.getCustomerId()).stream()
                    .filter(p -> !p.getId().equals(id))
                    .anyMatch(p -> p.getValidFrom().isAfter(price.getValidFrom())
                            && !p.getValidFrom().isAfter(req.validTo()));
            if (collides) {
                throw new BusinessException("종료일을 늘리면 다음 단가와 기간이 겹칩니다.",
                        ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        price.update(req.unitPrice(), req.currency(), req.incoterms(),
                req.minQty(), req.validTo(), req.remark());

        return toResponses(List.of(price)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        priceHistoryRepository.delete(get(id));
    }

    /** 특정 시점의 적용 단가를 미리 보여 준다 — 화면에서 "이 날짜엔 얼마?" 확인용 */
    @Transactional(readOnly = true)
    public ProductDtos.PriceResponse resolve(Long productId, Long customerId, LocalDate baseDate) {
        LocalDate target = baseDate != null ? baseDate : LocalDate.now();
        List<PriceHistory> candidates =
                priceHistoryRepository.findApplicable(productId, customerId, target);

        if (candidates.isEmpty()) {
            throw new NotFoundException("해당 날짜에 적용할 단가가 없습니다.");
        }
        return toResponses(List.of(candidates.get(0))).get(0);
    }

    private PriceHistory get(Long id) {
        return priceHistoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("단가를 찾을 수 없습니다. id=" + id));
    }

    private List<ProductDtos.PriceResponse> toResponses(List<PriceHistory> prices) {
        Map<Long, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        LocalDate today = LocalDate.now();

        return prices.stream()
                .map(p -> {
                    Product product = products.get(p.getProductId());
                    Customer customer = p.getCustomerId() == null ? null : customers.get(p.getCustomerId());
                    return new ProductDtos.PriceResponse(
                            p.getId(),
                            p.getProductId(),
                            product != null ? product.getProductCode() : null,
                            product != null ? product.getNameEn() : null,
                            p.getCustomerId(),
                            customer != null ? customer.getNameEn() : null,
                            p.isStandardPrice(),
                            p.getCurrency(),
                            p.getUnitPrice(),
                            p.getIncoterms(),
                            p.getMinQty(),
                            p.getValidFrom(),
                            p.getValidTo(),
                            p.isEffectiveOn(today),
                            p.getRemark());
                })
                .toList();
    }
}
