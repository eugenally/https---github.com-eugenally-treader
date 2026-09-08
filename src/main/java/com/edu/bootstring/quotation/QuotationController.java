package com.edu.bootstring.quotation;

import com.edu.bootstring.order.SalesOrderService;
import com.edu.bootstring.order.dto.OrderDtos;
import com.edu.bootstring.quotation.dto.QuotationDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final SalesOrderService salesOrderService;
    private final QuotationPdfService quotationPdfService;

    /** 견적서 PDF 다운로드 */
    @GetMapping("/{id}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        return com.edu.bootstring.global.document.FileResponses.pdf(
                quotationPdfService.render(id), quotationPdfService.fileName(id));
    }

    @GetMapping
    public List<QuotationDtos.SummaryResponse> findAll() {
        return quotationService.findAll();
    }

    @GetMapping("/{id}")
    public QuotationDtos.DetailResponse findOne(@PathVariable Long id) {
        return quotationService.findDetail(id);
    }

    @PostMapping
    public QuotationDtos.DetailResponse create(@Valid @RequestBody QuotationDtos.CreateRequest req) {
        return quotationService.create(req);
    }

    @PostMapping("/{id}/items")
    public QuotationDtos.DetailResponse addItem(@PathVariable Long id,
                                                @Valid @RequestBody QuotationDtos.AddItemRequest req) {
        return quotationService.addItem(id, req);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public QuotationDtos.DetailResponse removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return quotationService.removeItem(id, itemId);
    }

    @PostMapping("/{id}/send")
    public QuotationDtos.DetailResponse send(@PathVariable Long id) {
        return quotationService.send(id);
    }

    /** 제품을 고르는 순간 화면이 호출해 단가 입력칸을 채운다 */
    @GetMapping("/{id}/price-suggestion")
    public QuotationDtos.PriceSuggestion suggestPrice(@PathVariable Long id,
                                                      @RequestParam Long productId) {
        return quotationService.suggestPrice(id, productId);
    }

    /** 견적 → 수주 전환 */
    @PostMapping("/{id}/convert")
    public OrderDtos.DetailResponse convert(@PathVariable Long id,
                                            @RequestBody(required = false) OrderDtos.ConvertRequest req) {
        OrderDtos.ConvertRequest safe = req != null
                ? req
                : new OrderDtos.ConvertRequest(null, null, null);
        return salesOrderService.convertFromQuotation(id, safe);
    }
}
