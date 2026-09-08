package com.edu.bootstring.product;

import com.edu.bootstring.global.document.FileResponses;
import com.edu.bootstring.global.excel.dto.ExcelDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 마스터 데이터 엑셀 대량등록·내보내기.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/excel")
public class MasterExcelController {

    private final MasterExcelService excelService;

    // ------------------------------------------------------------------
    // 양식 다운로드
    // ------------------------------------------------------------------

    @GetMapping("/products/template")
    public ResponseEntity<byte[]> productTemplate() {
        return FileResponses.excel(excelService.productTemplate(), "제품등록양식.xlsx");
    }

    @GetMapping("/prices/template")
    public ResponseEntity<byte[]> priceTemplate() {
        return FileResponses.excel(excelService.priceTemplate(), "단가등록양식.xlsx");
    }

    // ------------------------------------------------------------------
    // 대량등록 — 부분 성공을 인정하고 실패 행만 사유와 함께 돌려준다
    // ------------------------------------------------------------------

    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelDtos.ImportResult importProducts(@RequestPart("file") MultipartFile file) {
        return excelService.importProducts(file);
    }

    @PostMapping(value = "/prices/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelDtos.ImportResult importPrices(@RequestPart("file") MultipartFile file) {
        return excelService.importPrices(file);
    }

    /** 실패 행만 엑셀로 다시 받아 그 행만 고쳐 재업로드할 수 있게 한다 */
    @PostMapping("/errors/export")
    public ResponseEntity<byte[]> exportErrors(@RequestPart("errors") ExcelDtos.RowError[] errors) {
        return FileResponses.excel(
                excelService.exportErrors(java.util.List.of(errors)), "업로드오류.xlsx");
    }

    // ------------------------------------------------------------------
    // 목록 내보내기
    // ------------------------------------------------------------------

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProducts() {
        return FileResponses.excel(excelService.exportProducts(), "제품목록.xlsx");
    }

    @GetMapping("/prices/export")
    public ResponseEntity<byte[]> exportPrices() {
        return FileResponses.excel(excelService.exportPrices(), "단가목록.xlsx");
    }
}
