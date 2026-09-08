package com.edu.bootstring.product;

import com.edu.bootstring.global.common.PageResponse;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import com.edu.bootstring.inventory.InventoryTxn;
import com.edu.bootstring.inventory.InventoryTxnRepository;
import com.edu.bootstring.product.dto.ProductDtos;
import com.edu.bootstring.quotation.QuotationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 제품 마스터와 재고 관리.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTxnRepository inventoryTxnRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final QuotationItemRepository quotationItemRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductDtos.Summary> search(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "productCode"));

        Page<Product> result = productRepository.search(
                blankToNull(keyword), blankToNull(status), pageable);

        Map<Long, Inventory> inventories = inventoryMap(result.getContent());
        return PageResponse.from(result, p -> toSummary(p, inventories.get(p.getId())));
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.Summary> findActive() {
        List<Product> products = productRepository.findAllByStatusOrderByProductCodeAsc("ACTIVE");
        Map<Long, Inventory> inventories = inventoryMap(products);
        return products.stream().map(p -> toSummary(p, inventories.get(p.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ProductDtos.Detail findOne(Long id) {
        return toDetail(get(id));
    }

    /**
     * 제품 등록. 재고 행을 함께 만든다.
     *
     * <p>재고 행이 없으면 출하 확정에서 "재고 정보가 없습니다"로 터진다.
     * 제품과 재고는 1:1 이므로 여기서 같이 만드는 게 맞다.
     */
    @Transactional
    public ProductDtos.Detail create(ProductDtos.CreateRequest req) {
        if (productRepository.existsByProductCode(req.productCode())) {
            throw new BusinessException("이미 사용 중인 제품 코드입니다: " + req.productCode(),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        Product product = productRepository.save(Product.builder()
                .productCode(req.productCode())
                .nameEn(req.nameEn())
                .nameKo(req.nameKo())
                .spec(req.spec())
                .hsCode(req.hsCode())
                .unit(req.unit())
                .moq(req.moq())
                .packageType(req.packageType())
                .qtyPerCarton(req.qtyPerCarton())
                .netWeight(req.netWeight())
                .grossWeight(req.grossWeight())
                .cbm(req.cbm())
                .build());

        BigDecimal initial = req.initialStock() != null ? req.initialStock() : BigDecimal.ZERO;
        inventoryRepository.save(new Inventory(product.getId(), initial));

        if (initial.signum() > 0) {
            inventoryTxnRepository.save(InventoryTxn.builder()
                    .productId(product.getId())
                    .txnType("IN")
                    .qty(initial)
                    .beforeQty(BigDecimal.ZERO)
                    .afterQty(initial)
                    .refType("PRODUCT")
                    .refId(product.getId())
                    .build());
        }

        return toDetail(product);
    }

    @Transactional
    public ProductDtos.Detail update(Long id, ProductDtos.UpdateRequest req) {
        Product product = get(id);
        product.update(req.nameEn(), req.nameKo(), req.spec(), req.hsCode(), req.unit(),
                req.moq(), req.packageType(), req.qtyPerCarton(),
                req.netWeight(), req.grossWeight(), req.cbm());
        return toDetail(product);
    }

    /**
     * 삭제.
     *
     * <p>물리 삭제는 <b>흔적이 전혀 없을 때만</b> 한다. 잘못 만든 제품을 바로 지우는 경우다.
     * 아래 중 하나라도 있으면 비활성으로 돌린다.
     * <ul>
     *   <li>견적·수주 라인에 쓰임 — 지우면 과거 문서가 깨진다</li>
     *   <li>재고 변동 이력 — <b>감사 추적</b>이라 제품과 함께 지울 성격이 아니다</li>
     * </ul>
     * 재고 행과 단가는 제품에 딸린 것이라 함께 지운다.
     */
    @Transactional
    public String delete(Long id) {
        Product product = get(id);

        if (isInUse(product.getId())) {
            product.deactivate();
            return "견적·수주에 사용된 제품이라 삭제하지 않고 비활성 처리했습니다.";
        }
        if (inventoryTxnRepository.countByProductId(product.getId()) > 0) {
            product.deactivate();
            return "재고 변동 이력이 있어 삭제하지 않고 비활성 처리했습니다.";
        }

        // 재고·단가 행을 먼저 정리해야 FK 가 걸리지 않는다.
        // 단가는 거래처 전용까지 전부 지워야 한다 (findSeries 는 표준 단가만 준다).
        inventoryRepository.findById(product.getId()).ifPresent(inventoryRepository::delete);
        priceHistoryRepository.deleteAll(priceHistoryRepository.findAllByProductId(product.getId()));
        productRepository.delete(product);
        return "제품을 삭제했습니다.";
    }

    @Transactional
    public ProductDtos.Detail changeStatus(Long id, boolean active) {
        Product product = get(id);
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        return toDetail(product);
    }

    // ------------------------------------------------------------------
    // 재고
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProductDtos.StockResponse> findStocks(String keyword) {
        List<Product> products = blankToNull(keyword) == null
                ? productRepository.findAllByStatusOrderByProductCodeAsc("ACTIVE")
                : productRepository.search(keyword.trim(), "ACTIVE",
                        PageRequest.of(0, 500, Sort.by("productCode"))).getContent();

        Map<Long, Inventory> inventories = inventoryMap(products);

        return products.stream()
                .map(p -> {
                    Inventory inv = inventories.get(p.getId());
                    return new ProductDtos.StockResponse(
                            p.getId(), p.getProductCode(), p.getNameEn(), p.getUnit(),
                            inv != null ? inv.getOnHandQty() : BigDecimal.ZERO,
                            inv != null ? inv.getAllocatedQty() : BigDecimal.ZERO,
                            inv != null ? inv.getAvailableQty() : BigDecimal.ZERO,
                            inv != null ? inv.getSafetyQty() : BigDecimal.ZERO,
                            inv != null && inv.isBelowSafety());
                })
                .toList();
    }

    /**
     * 재고 실사 조정.
     *
     * <p>차이 수량이 아니라 <b>실제로 센 수량</b>을 받는다. 차이를 입력받으면 부호를 헷갈려
     * 반대로 넣는 사고가 난다. 조정분은 {@code ADJUST} 로 이력에 남긴다.
     */
    @Transactional
    public ProductDtos.StockResponse adjustStock(Long productId, ProductDtos.StockAdjustRequest req) {
        Product product = get(productId);
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> inventoryRepository.save(new Inventory(productId, BigDecimal.ZERO)));

        BigDecimal before = inventory.adjustTo(req.countedQty());
        BigDecimal diff = req.countedQty().subtract(before);

        if (diff.signum() != 0) {
            inventoryTxnRepository.save(InventoryTxn.builder()
                    .productId(productId)
                    .txnType("ADJUST")
                    .qty(diff)
                    .beforeQty(before)
                    .afterQty(req.countedQty())
                    .refType("PRODUCT")
                    .refId(productId)
                    .build());
        }

        return new ProductDtos.StockResponse(
                product.getId(), product.getProductCode(), product.getNameEn(), product.getUnit(),
                inventory.getOnHandQty(), inventory.getAllocatedQty(), inventory.getAvailableQty(),
                inventory.getSafetyQty(), inventory.isBelowSafety());
    }

    // ------------------------------------------------------------------
    // 내부
    // ------------------------------------------------------------------

    /** 견적·수주 라인에 쓰였으면 사용 중이다 */
    private boolean isInUse(Long productId) {
        return quotationItemRepository.countByProductId(productId) > 0;
    }

    Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다. id=" + id));
    }

    private Map<Long, Inventory> inventoryMap(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        return inventoryRepository.findAllByProductIdIn(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
    }

    private ProductDtos.Summary toSummary(Product p, Inventory inv) {
        return new ProductDtos.Summary(
                p.getId(), p.getProductCode(), p.getNameEn(), p.getNameKo(), p.getSpec(),
                p.getUnit(), p.getMoq(), p.getNetWeight(), p.getGrossWeight(), p.getCbm(),
                p.getStatus(),
                inv != null ? inv.getOnHandQty() : BigDecimal.ZERO,
                inv != null ? inv.getAllocatedQty() : BigDecimal.ZERO,
                inv != null ? inv.getAvailableQty() : BigDecimal.ZERO,
                isDeletable(p.getId()));
    }

    /** 물리 삭제가 가능한가 — 화면의 삭제 버튼 문구를 가르는 값 */
    private boolean isDeletable(Long productId) {
        return !isInUse(productId) && inventoryTxnRepository.countByProductId(productId) == 0;
    }

    private ProductDtos.Detail toDetail(Product p) {
        Inventory inv = inventoryRepository.findById(p.getId()).orElse(null);
        long usageCount = quotationItemRepository.countByProductId(p.getId());
        long priceCount = priceHistoryRepository.countByProductId(p.getId());

        return new ProductDtos.Detail(
                p.getId(), p.getProductCode(), p.getNameEn(), p.getNameKo(), p.getSpec(),
                p.getHsCode(), p.getUnit(), p.getMoq(), p.getPackageType(), p.getQtyPerCarton(),
                p.getNetWeight(), p.getGrossWeight(), p.getCbm(), p.getStatus(),
                inv != null ? inv.getOnHandQty() : BigDecimal.ZERO,
                inv != null ? inv.getAllocatedQty() : BigDecimal.ZERO,
                inv != null ? inv.getAvailableQty() : BigDecimal.ZERO,
                isDeletable(p.getId()), priceCount, usageCount);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
