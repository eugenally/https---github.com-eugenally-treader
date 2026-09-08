package com.edu.bootstring.quotation;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.document.PdfRenderer;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 견적서 PDF (Quotation).
 */
@Service
@RequiredArgsConstructor
public class QuotationPdfService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PdfRenderer pdf;

    @Transactional(readOnly = true)
    public byte[] render(Long quotationId) {
        Quotation q = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new NotFoundException("견적을 찾을 수 없습니다. id=" + quotationId));
        Customer customer = customerRepository.findById(q.getCustomerId()).orElse(null);
        Map<Long, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        StringBuilder sb = pdf.openDocument("QUOTATION");

        // 수신처 / 문서정보
        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td width=\"55%\"><div class=\"box\">");
        sb.append("<div class=\"label\">Messrs (Buyer)</div>");
        sb.append("<div><strong>")
                .append(pdf.escape(customer == null ? "-" : customer.getNameEn()))
                .append("</strong></div>");
        if (customer != null && customer.getAddress() != null) {
            sb.append("<div>").append(pdf.escape(customer.getAddress())).append("</div>");
        }
        if (customer != null) {
            sb.append("<div>").append(pdf.escape(customer.getCountryCode())).append("</div>");
        }
        sb.append("</div></td>");

        sb.append("<td width=\"3%\"></td><td width=\"42%\"><div class=\"box\">");
        pdf.labelledLine(sb, "Quotation No.", q.getQuoteNo());
        pdf.labelledLine(sb, "Date", pdf.date(q.getQuoteDate()));
        pdf.labelledLine(sb, "Valid Until", pdf.date(q.getValidUntil()));
        pdf.labelledLine(sb, "Incoterms", q.getIncoterms());
        pdf.labelledLine(sb, "Payment Terms", q.getPaymentTermsCode());
        sb.append("</div></td></tr></table>");

        // 선적 조건
        sb.append("<table class=\"meta\"><tr><td><div class=\"box\"><table><tr>");
        pdf.labelledCell(sb, "Port of Loading", q.getPortOfLoading());
        pdf.labelledCell(sb, "Port of Discharge", q.getPortOfDischarge());
        pdf.labelledCell(sb, "Currency", q.getCurrency());
        sb.append("</tr></table></div></td></tr></table>");

        // 명세
        sb.append("<table class=\"items\"><tr>");
        sb.append("<th width=\"7%\">No</th><th width=\"18%\">Code</th><th>Description</th>");
        sb.append("<th width=\"13%\">Qty</th><th width=\"15%\">Unit Price</th><th width=\"17%\">Amount</th>");
        sb.append("</tr>");

        for (QuotationItem item : q.getItems()) {
            Product p = products.get(item.getProductId());
            sb.append("<tr>");
            sb.append("<td class=\"c\">").append(item.getLineNo()).append("</td>");
            sb.append("<td>").append(pdf.escape(p != null ? p.getProductCode() : "-")).append("</td>");
            sb.append("<td>").append(pdf.escape(describe(p))).append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getQty(), 0)).append(' ')
                    .append(pdf.escape(p != null ? p.getUnit() : "")).append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getUnitPrice(), 2)).append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getAmount(), 2)).append("</td>");
            sb.append("</tr>");
        }

        sb.append("<tr class=\"total\"><td colspan=\"5\" class=\"r\">TOTAL (")
                .append(pdf.escape(q.getCurrency())).append(")</td><td class=\"r\">")
                .append(pdf.number(q.getTotalAmount(), 2)).append("</td></tr>");

        if (q.getKrwAmount() != null) {
            sb.append("<tr><td colspan=\"5\" class=\"r\">Equivalent (KRW @ ")
                    .append(pdf.number(q.getExchangeRate(), 2)).append(")</td><td class=\"r\">")
                    .append(pdf.number(q.getKrwAmount(), 0)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<div class=\"foot\">");
        sb.append("<div>This quotation is valid until <strong>")
                .append(pdf.date(q.getValidUntil()))
                .append("</strong>. Prices are subject to change after this date.</div>");
        if (q.getRemark() != null && !q.getRemark().isBlank()) {
            sb.append("<div style=\"margin-top:2mm\"><strong>Remarks:</strong> ")
                    .append(pdf.escape(q.getRemark())).append("</div>");
        }
        sb.append("</div>");

        pdf.closeDocument(sb);
        return pdf.render(sb.toString());
    }

    @Transactional(readOnly = true)
    public String fileName(Long quotationId) {
        return quotationRepository.findById(quotationId)
                .map(q -> q.getQuoteNo() + ".pdf")
                .orElse("quotation.pdf");
    }

    private String describe(Product p) {
        if (p == null) {
            return "-";
        }
        return p.getSpec() == null || p.getSpec().isBlank()
                ? p.getNameEn()
                : p.getNameEn() + " (" + p.getSpec() + ")";
    }
}
