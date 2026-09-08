package com.edu.bootstring.product;

import com.edu.bootstring.product.dto.ProductDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prices")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    /** 제품·거래처로 걸러 본다. 둘 다 비우면 전체 */
    @GetMapping
    public List<ProductDtos.PriceResponse> findList(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long customerId) {
        return priceHistoryService.findList(productId, customerId);
    }

    /** 특정 날짜의 적용 단가 미리보기 */
    @GetMapping("/resolve")
    public ProductDtos.PriceResponse resolve(
            @RequestParam Long productId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return priceHistoryService.resolve(productId, customerId, baseDate);
    }

    @PostMapping
    public ProductDtos.PriceResponse create(@Valid @RequestBody ProductDtos.PriceCreateRequest req) {
        return priceHistoryService.create(req);
    }

    @PutMapping("/{id}")
    public ProductDtos.PriceResponse update(@PathVariable Long id,
                                            @Valid @RequestBody ProductDtos.PriceUpdateRequest req) {
        return priceHistoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        priceHistoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
