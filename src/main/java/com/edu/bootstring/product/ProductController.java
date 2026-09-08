package com.edu.bootstring.product;

import com.edu.bootstring.global.common.PageResponse;
import com.edu.bootstring.product.dto.ProductDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductDtos.Summary> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return productService.search(page, size, keyword, status);
    }

    /** 활성 제품 전체 — 견적 품목 선택 모달이 쓴다 */
    @GetMapping("/active")
    public List<ProductDtos.Summary> findActive() {
        return productService.findActive();
    }

    @GetMapping("/{id}")
    public ProductDtos.Detail findOne(@PathVariable Long id) {
        return productService.findOne(id);
    }

    @PostMapping
    public ProductDtos.Detail create(@Valid @RequestBody ProductDtos.CreateRequest req) {
        return productService.create(req);
    }

    @PutMapping("/{id}")
    public ProductDtos.Detail update(@PathVariable Long id,
                                     @Valid @RequestBody ProductDtos.UpdateRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        return Map.of("message", productService.delete(id));
    }

    @PostMapping("/{id}/status")
    public ProductDtos.Detail changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        return productService.changeStatus(id, active);
    }

    // ------------------------------------------------------------------
    // 재고
    // ------------------------------------------------------------------

    @GetMapping("/stocks")
    public List<ProductDtos.StockResponse> stocks(@RequestParam(required = false) String keyword) {
        return productService.findStocks(keyword);
    }

    /** 실사 조정 — 센 수량으로 덮는다 */
    @PostMapping("/{id}/stock-adjust")
    public ProductDtos.StockResponse adjustStock(@PathVariable Long id,
                                                 @Valid @RequestBody ProductDtos.StockAdjustRequest req) {
        return productService.adjustStock(id, req);
    }
}
