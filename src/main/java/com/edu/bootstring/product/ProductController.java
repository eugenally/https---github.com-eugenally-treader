package com.edu.bootstring.product;

import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /** 제품 선택 모달이 쓰는 응답. 재고를 함께 실어 보내 화면에서 가용량을 바로 보여준다. */
    public record ProductResponse(
            Long id,
            String productCode,
            String nameEn,
            String nameKo,
            String spec,
            String unit,
            BigDecimal moq,
            BigDecimal netWeight,
            BigDecimal grossWeight,
            BigDecimal cbm,
            BigDecimal onHandQty,
            BigDecimal availableQty
    ) {
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        List<Product> products = productRepository.findAllByStatusOrderByProductCodeAsc("ACTIVE");

        Map<Long, Inventory> inventories = inventoryRepository
                .findAllByProductIdIn(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        return products.stream()
                .map(p -> {
                    Inventory inv = inventories.get(p.getId());
                    return new ProductResponse(
                            p.getId(),
                            p.getProductCode(),
                            p.getNameEn(),
                            p.getNameKo(),
                            p.getSpec(),
                            p.getUnit(),
                            p.getMoq(),
                            p.getNetWeight(),
                            p.getGrossWeight(),
                            p.getCbm(),
                            inv != null ? inv.getOnHandQty() : null,
                            inv != null ? inv.getAvailableQty() : null);
                })
                .toList();
    }
}
