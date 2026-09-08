package com.edu.bootstring.product;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.excel.ExcelReader;
import com.edu.bootstring.global.excel.ExcelWriter;
import com.edu.bootstring.global.excel.dto.ExcelDtos;
import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import com.edu.bootstring.inventory.InventoryTxn;
import com.edu.bootstring.inventory.InventoryTxnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 제품·단가 엑셀 대량등록과 내보내기.
 *
 * <p><b>부분 실패를 인정한다.</b> 100건 중 3건이 틀렸다고 97건을 되돌리면
 * 사용자는 파일을 통째로 고쳐 처음부터 다시 올려야 한다.
 * 성공한 건 넣고, 실패한 건 행 번호와 사유를 돌려줘 그 행만 고치게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterExcelService {

    private static final List<String> PRODUCT_HEADERS =
            List.of("제품코드", "영문품명", "단위");
    private static final List<String> PRICE_HEADERS =
            List.of("제품코드", "단가", "적용시작일");

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTxnRepository inventoryTxnRepository;
    private final MasterExcelRowWriter rowWriter;

    // ------------------------------------------------------------------
    // 제품 대량등록
    // ------------------------------------------------------------------

    public ExcelDtos.ImportResult importProducts(MultipartFile file) {
        List<ExcelReader.RowData> rows = ExcelReader.read(file, PRODUCT_HEADERS);

        List<ExcelDtos.RowError> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        int success = 0;
        int skipped = 0;

        for (ExcelReader.RowData row : rows) {
            String code = row.str("제품코드");
            try {
                if (code == null) {
                    throw new IllegalArgumentException("제품코드는 필수입니다.");
                }
                // 파일 안에서의 중복도 잡아야 한다. DB 확인만으로는 같은 파일의 중복을 놓친다.
                if (!seenCodes.add(code)) {
                    throw new IllegalArgumentException("파일 안에 같은 제품코드가 중복됩니다.");
                }
                if (productRepository.existsByProductCode(code)) {
                    skipped++;
                    continue;
                }

                rowWriter.saveProduct(row, code);
                success++;

            } catch (Exception e) {
                errors.add(new ExcelDtos.RowError(row.rowNum(), code, rootMessage(e)));
            }
        }

        return new ExcelDtos.ImportResult(rows.size(), success, errors.size(), skipped, errors);
    }


    // ------------------------------------------------------------------
    // 단가 대량등록
    // ------------------------------------------------------------------

    public ExcelDtos.ImportResult importPrices(MultipartFile file) {
        List<ExcelReader.RowData> rows = ExcelReader.read(file, PRICE_HEADERS);

        Map<String, Product> productsByCode = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductCode, Function.identity()));
        Map<String, Customer> customersByCode = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getCustomerCode, Function.identity()));

        List<ExcelDtos.RowError> errors = new ArrayList<>();
        int success = 0;

        for (ExcelReader.RowData row : rows) {
            String productCode = row.str("제품코드");
            try {
                Product product = productsByCode.get(productCode);
                if (product == null) {
                    throw new IllegalArgumentException("등록되지 않은 제품코드입니다: " + productCode);
                }

                String customerCode = row.str("거래처코드");
                Long customerId = null;
                if (customerCode != null) {
                    Customer customer = customersByCode.get(customerCode);
                    if (customer == null) {
                        throw new IllegalArgumentException("등록되지 않은 거래처코드입니다: " + customerCode);
                    }
                    customerId = customer.getId();
                }

                BigDecimal unitPrice = row.decimal("단가");
                if (unitPrice == null || unitPrice.signum() <= 0) {
                    throw new IllegalArgumentException("단가는 0보다 커야 합니다.");
                }
                LocalDate validFrom = row.requiredDate("적용시작일");
                LocalDate validTo = row.date("적용종료일");
                if (validTo != null && validTo.isBefore(validFrom)) {
                    throw new IllegalArgumentException("적용종료일이 시작일보다 빠릅니다.");
                }

                rowWriter.savePrice(product.getId(), customerId, row, unitPrice, validFrom, validTo);
                success++;

            } catch (Exception e) {
                errors.add(new ExcelDtos.RowError(row.rowNum(), productCode, rootMessage(e)));
            }
        }

        return new ExcelDtos.ImportResult(rows.size(), success, errors.size(), 0, errors);
    }


    // ------------------------------------------------------------------
    // 양식 다운로드
    // ------------------------------------------------------------------

    public byte[] productTemplate() {
        List<ExcelWriter.Column<Object>> columns = List.of(
                ExcelWriter.Column.of("제품코드", o -> null, 18),
                ExcelWriter.Column.of("영문품명", o -> null, 30),
                ExcelWriter.Column.of("한글품명", o -> null, 24),
                ExcelWriter.Column.of("규격", o -> null, 24),
                ExcelWriter.Column.of("HS코드", o -> null),
                ExcelWriter.Column.of("단위", o -> null, 10),
                ExcelWriter.Column.of("MOQ", o -> null, 12),
                ExcelWriter.Column.of("포장형태", o -> null),
                ExcelWriter.Column.of("박스당수량", o -> null),
                ExcelWriter.Column.of("순중량", o -> null),
                ExcelWriter.Column.of("총중량", o -> null),
                ExcelWriter.Column.of("CBM", o -> null),
                ExcelWriter.Column.of("초기재고", o -> null));

        return ExcelWriter.write("제품등록양식", columns, List.of(),
                "※ 제품코드·영문품명·단위는 필수입니다. 이미 있는 제품코드는 건너뜁니다. 이 안내 행은 지우지 마세요.");
    }

    public byte[] priceTemplate() {
        List<ExcelWriter.Column<Object>> columns = List.of(
                ExcelWriter.Column.of("제품코드", o -> null, 18),
                ExcelWriter.Column.of("거래처코드", o -> null, 14),
                ExcelWriter.Column.of("통화", o -> null, 10),
                ExcelWriter.Column.of("단가", o -> null, 14),
                ExcelWriter.Column.of("Incoterms", o -> null, 12),
                ExcelWriter.Column.of("최소수량", o -> null),
                ExcelWriter.Column.of("적용시작일", o -> null, 14),
                ExcelWriter.Column.of("적용종료일", o -> null, 14),
                ExcelWriter.Column.of("비고", o -> null, 24));

        return ExcelWriter.write("단가등록양식", columns, List.of(),
                "※ 거래처코드를 비우면 표준 단가가 됩니다. 날짜는 yyyy-MM-dd. 기간이 겹치면 이전 단가가 자동 마감됩니다.");
    }

    // ------------------------------------------------------------------
    // 목록 내보내기
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] exportProducts() {
        List<Product> products = productRepository.findAll();
        Map<Long, Inventory> inventories = inventoryRepository
                .findAllByProductIdIn(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        List<ExcelWriter.Column<Product>> columns = List.of(
                ExcelWriter.Column.of("제품코드", Product::getProductCode, 18),
                ExcelWriter.Column.of("영문품명", Product::getNameEn, 30),
                ExcelWriter.Column.of("한글품명", Product::getNameKo, 24),
                ExcelWriter.Column.of("규격", Product::getSpec, 24),
                ExcelWriter.Column.of("HS코드", Product::getHsCode),
                ExcelWriter.Column.of("단위", Product::getUnit, 10),
                ExcelWriter.Column.of("MOQ", Product::getMoq, 12),
                ExcelWriter.Column.of("순중량", Product::getNetWeight),
                ExcelWriter.Column.of("총중량", Product::getGrossWeight),
                ExcelWriter.Column.of("CBM", Product::getCbm),
                ExcelWriter.Column.of("현재고",
                        p -> value(inventories.get(p.getId()), Inventory::getOnHandQty)),
                ExcelWriter.Column.of("할당",
                        p -> value(inventories.get(p.getId()), Inventory::getAllocatedQty)),
                ExcelWriter.Column.of("가용",
                        p -> value(inventories.get(p.getId()), Inventory::getAvailableQty)),
                ExcelWriter.Column.of("상태", Product::getStatus, 10));

        return ExcelWriter.write("제품목록", columns, products);
    }

    @Transactional(readOnly = true)
    public byte[] exportPrices() {
        List<PriceHistory> prices = priceHistoryRepository.findForList(null, null);
        Map<Long, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        LocalDate today = LocalDate.now();

        List<ExcelWriter.Column<PriceHistory>> columns = List.of(
                ExcelWriter.Column.of("제품코드",
                        p -> value(products.get(p.getProductId()), Product::getProductCode), 18),
                ExcelWriter.Column.of("품명",
                        p -> value(products.get(p.getProductId()), Product::getNameEn), 30),
                ExcelWriter.Column.of("거래처코드",
                        p -> p.getCustomerId() == null ? "(표준)"
                                : value(customers.get(p.getCustomerId()), Customer::getCustomerCode), 14),
                ExcelWriter.Column.of("통화", PriceHistory::getCurrency, 10),
                ExcelWriter.Column.of("단가", PriceHistory::getUnitPrice, 14),
                ExcelWriter.Column.of("Incoterms", PriceHistory::getIncoterms, 12),
                ExcelWriter.Column.of("적용시작일", PriceHistory::getValidFrom, 14),
                ExcelWriter.Column.of("적용종료일",
                        p -> p.getValidTo() != null ? p.getValidTo() : "무기한", 14),
                ExcelWriter.Column.of("현재적용", p -> p.isEffectiveOn(today) ? "Y" : "N", 10));

        return ExcelWriter.write("단가목록", columns, prices);
    }

    /** 실패 행만 모아 다시 내려 준다 — 사용자는 이 파일만 고쳐 재업로드하면 된다 */
    public byte[] exportErrors(List<ExcelDtos.RowError> errors) {
        List<ExcelWriter.Column<ExcelDtos.RowError>> columns = List.of(
                ExcelWriter.Column.of("엑셀 행번호", ExcelDtos.RowError::rowNum, 12),
                ExcelWriter.Column.of("키", ExcelDtos.RowError::key, 20),
                ExcelWriter.Column.of("실패 사유", ExcelDtos.RowError::reason, 60));

        return ExcelWriter.write("업로드오류", columns, errors,
                "※ 아래 행만 원본 파일에서 고쳐 다시 올리시면 됩니다.");
    }

    // ------------------------------------------------------------------

    private <T, R> R value(T source, Function<T, R> getter) {
        return source == null ? null : getter.apply(source);
    }

    /**
     * 사용자에게 보여줄 실패 사유.
     * DB 제약 위반 같은 건 메시지가 길고 기술적이라 앞부분만 잘라 쓴다.
     */
    private String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getMessage() == null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        int newline = message.indexOf('\n');
        if (newline > 0) {
            message = message.substring(0, newline);
        }
        return message.length() > 200 ? message.substring(0, 200) + "…" : message;
    }
}
