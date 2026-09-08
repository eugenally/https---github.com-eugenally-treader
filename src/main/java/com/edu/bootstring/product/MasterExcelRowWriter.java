package com.edu.bootstring.product;

import com.edu.bootstring.global.excel.ExcelReader;
import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import com.edu.bootstring.inventory.InventoryTxn;
import com.edu.bootstring.inventory.InventoryTxnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 엑셀 대량등록에서 <b>행 하나</b>를 저장한다.
 *
 * <p>왜 별도 빈인가 — {@code REQUIRES_NEW} 는 프록시를 거쳐야 동작한다.
 * 같은 클래스 안에서 {@code this.saveRow()} 로 부르면 AOP 를 타지 않아
 * 바깥 트랜잭션에 그대로 합류하고, 한 행이 터지면 앞서 넣은 행까지 함께 롤백된다.
 * 자기 주입으로도 풀 수 있지만 순환 참조를 만들므로 책임을 나누는 편이 낫다.
 */
@Service
@RequiredArgsConstructor
public class MasterExcelRowWriter {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /** 제품 한 행. 이 메서드가 던지면 이 행만 롤백되고 나머지는 남는다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveProduct(ExcelReader.RowData row, String code) {
        Product product = productRepository.save(Product.builder()
                .productCode(code)
                .nameEn(row.required("영문품명"))
                .nameKo(row.str("한글품명"))
                .spec(row.str("규격"))
                .hsCode(row.str("HS코드"))
                .unit(row.required("단위"))
                .moq(row.decimal("MOQ"))
                .packageType(row.str("포장형태"))
                .qtyPerCarton(row.integer("박스당수량"))
                .netWeight(row.decimal("순중량"))
                .grossWeight(row.decimal("총중량"))
                .cbm(row.decimal("CBM"))
                .build());

        BigDecimal initial = row.decimal("초기재고");
        BigDecimal stock = initial != null ? initial : BigDecimal.ZERO;
        if (stock.signum() < 0) {
            throw new IllegalArgumentException("초기재고는 0 이상이어야 합니다.");
        }

        // 재고 행은 제품과 함께 만든다. 없으면 나중에 출하 확정에서 터진다.
        inventoryRepository.save(new Inventory(product.getId(), stock));
        if (stock.signum() > 0) {
            inventoryTxnRepository.save(InventoryTxn.builder()
                    .productId(product.getId())
                    .txnType("IN")
                    .qty(stock)
                    .beforeQty(BigDecimal.ZERO)
                    .afterQty(stock)
                    .refType("PRODUCT")
                    .refId(product.getId())
                    .build());
        }
    }

    /** 단가 한 행. 화면 등록과 같은 규칙으로 겹치는 이전 단가를 마감한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePrice(Long productId, Long customerId, ExcelReader.RowData row,
                          BigDecimal unitPrice, LocalDate validFrom, LocalDate validTo) {

        List<PriceHistory> overlapping = priceHistoryRepository.findSeries(productId, customerId).stream()
                .filter(p -> p.overlaps(validFrom, validTo))
                .toList();

        // 새 단가보다 뒤에 시작하는 단가는 뒤로 밀 수 없으므로 거절한다
        List<PriceHistory> future = overlapping.stream()
                .filter(p -> !p.getValidFrom().isBefore(validFrom))
                .toList();
        if (!future.isEmpty()) {
            throw new IllegalArgumentException(
                    "%s 부터 시작하는 단가가 이미 있습니다.".formatted(future.get(0).getValidFrom()));
        }
        overlapping.forEach(p -> p.closeBefore(validFrom));
        priceHistoryRepository.saveAll(overlapping);

        priceHistoryRepository.save(PriceHistory.builder()
                .productId(productId)
                .customerId(customerId)
                .currency(row.str("통화") != null ? row.str("통화") : "USD")
                .unitPrice(unitPrice)
                .incoterms(row.str("Incoterms"))
                .minQty(row.decimal("최소수량"))
                .validFrom(validFrom)
                .validTo(validTo)
                .remark(row.str("비고"))
                .build());
    }
}
