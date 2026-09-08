package com.edu.bootstring.quotation;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.docnumber.DocNumberService;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.PriceHistory;
import com.edu.bootstring.product.PriceLookupService;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import com.edu.bootstring.quotation.dto.QuotationDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 견적 작성과 상태 전이를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PriceLookupService priceLookupService;
    private final DocNumberService docNumberService;

    @Transactional(readOnly = true)
    public List<QuotationDtos.SummaryResponse> findAll() {
        Map<Long, Customer> customers = customerMap();

        return quotationRepository.findAllByOrderByIdDesc().stream()
                .map(q -> new QuotationDtos.SummaryResponse(
                        q.getId(),
                        q.getQuoteNo(),
                        q.getRevNo(),
                        q.getCustomerId(),
                        customerName(customers, q.getCustomerId()),
                        q.getStatus().name(),
                        q.getCurrency(),
                        q.getTotalAmount(),
                        q.getQuoteDate(),
                        q.getValidUntil(),
                        salesOrderRepository.existsByQuotationId(q.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotationDtos.DetailResponse findDetail(Long id) {
        Quotation q = get(id);
        return toDetail(q);
    }

    /** 새 견적. 유효기한을 비우면 거래처의 기본 일수를 쓴다 (D5-01). */
    @Transactional
    public QuotationDtos.DetailResponse create(QuotationDtos.CreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다."));

        LocalDate quoteDate = req.quoteDate() != null ? req.quoteDate() : LocalDate.now();
        LocalDate validUntil = req.validUntil() != null
                ? req.validUntil()
                : quoteDate.plusDays(customer.getQuoteValidDays());

        Quotation quotation = Quotation.builder()
                .quoteNo(docNumberService.next("QT", customer.getCustomerCode()))
                .customerId(customer.getId())
                .quoteDate(quoteDate)
                .validUntil(validUntil)
                .currency(orDefault(req.currency(), customer.getDefaultCurrency()))
                .incoterms(orDefault(req.incoterms(), customer.getDefaultIncoterms()))
                .portOfLoading(req.portOfLoading())
                .portOfDischarge(req.portOfDischarge())
                .paymentTermsCode(customer.getPaymentTermsCode())
                .remark(req.remark())
                .build();

        return toDetail(quotationRepository.save(quotation));
    }

    /** 품목 추가. 단가를 비우면 단가 이력에서 자동으로 채운다. */
    @Transactional
    public QuotationDtos.DetailResponse addItem(Long quotationId, QuotationDtos.AddItemRequest req) {
        Quotation quotation = get(quotationId);
        requireEditable(quotation);

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));

        BigDecimal unitPrice = req.unitPrice() != null
                ? req.unitPrice()
                : priceLookupService
                .findApplicable(product.getId(), quotation.getCustomerId(), quotation.getQuoteDate())
                .map(PriceHistory::getUnitPrice)
                .orElseThrow(() -> new BusinessException(
                        "등록된 단가가 없습니다. 단가를 직접 입력해 주세요.", ErrorCode.INVALID_INPUT_VALUE));

        int nextLineNo = quotation.getItems().stream()
                .map(QuotationItem::getLineNo)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        quotation.addItem(QuotationItem.builder()
                .lineNo(nextLineNo)
                .productId(product.getId())
                .qty(req.qty())
                .unitPrice(unitPrice)
                .remark(req.remark())
                .build());
        quotation.recalcTotal();

        return toDetail(quotation);
    }

    @Transactional
    public QuotationDtos.DetailResponse removeItem(Long quotationId, Long itemId) {
        Quotation quotation = get(quotationId);
        requireEditable(quotation);

        boolean removed = quotation.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new NotFoundException("견적 품목을 찾을 수 없습니다.");
        }
        quotation.recalcTotal();

        return toDetail(quotation);
    }

    /** 바이어에게 발송 — DRAFT → SENT */
    @Transactional
    public QuotationDtos.DetailResponse send(Long id) {
        Quotation quotation = get(id);
        if (quotation.getItems().isEmpty()) {
            throw new BusinessException("품목이 없는 견적은 발송할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        quotation.changeStatus(QuotationStatus.SENT);
        return toDetail(quotation);
    }

    /** 단가 자동 제안 — 화면에서 제품을 고르는 순간 호출된다 */
    @Transactional(readOnly = true)
    public QuotationDtos.PriceSuggestion suggestPrice(Long quotationId, Long productId) {
        Quotation quotation = get(quotationId);

        return priceLookupService
                .findApplicable(productId, quotation.getCustomerId(), quotation.getQuoteDate())
                .map(p -> new QuotationDtos.PriceSuggestion(
                        productId,
                        p.getUnitPrice(),
                        p.getCurrency(),
                        p.getCustomerId() == null ? "STANDARD" : "CUSTOMER"))
                .orElse(new QuotationDtos.PriceSuggestion(productId, null, quotation.getCurrency(), "NONE"));
    }

    private Quotation get(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("견적을 찾을 수 없습니다. id=" + id));
    }

    private void requireEditable(Quotation quotation) {
        if (!quotation.isEditable()) {
            throw new BusinessException(
                    "%s 상태의 견적은 수정할 수 없습니다.".formatted(quotation.getStatus()),
                    ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    private QuotationDtos.DetailResponse toDetail(Quotation q) {
        Map<Long, Customer> customers = customerMap();
        Map<Long, Product> products = productMap();

        List<QuotationDtos.ItemResponse> items = q.getItems().stream()
                .map(i -> {
                    Product p = products.get(i.getProductId());
                    return new QuotationDtos.ItemResponse(
                            i.getId(),
                            i.getLineNo(),
                            i.getProductId(),
                            p != null ? p.getProductCode() : null,
                            p != null ? p.getNameEn() : null,
                            p != null ? p.getUnit() : null,
                            i.getQty(),
                            i.getUnitPrice(),
                            i.getAmount());
                })
                .toList();

        Long convertedOrderId = salesOrderRepository.findAllByOrderByIdDesc().stream()
                .filter(o -> q.getId().equals(o.getQuotationId()))
                .map(o -> o.getId())
                .findFirst()
                .orElse(null);

        return new QuotationDtos.DetailResponse(
                q.getId(),
                q.getQuoteNo(),
                q.getRevNo(),
                q.getCustomerId(),
                customerName(customers, q.getCustomerId()),
                q.getStatus().name(),
                q.getCurrency(),
                q.getIncoterms(),
                q.getPortOfLoading(),
                q.getPortOfDischarge(),
                q.getQuoteDate(),
                q.getValidUntil(),
                q.getTotalAmount(),
                q.getExchangeRate(),
                q.getKrwAmount(),
                q.getRemark(),
                q.isEditable(),
                convertedOrderId != null,
                convertedOrderId,
                items);
    }

    private Map<Long, Customer> customerMap() {
        return customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private Map<Long, Product> productMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String customerName(Map<Long, Customer> customers, Long customerId) {
        Customer c = customers.get(customerId);
        return c != null ? c.getNameEn() : null;
    }

    private String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
