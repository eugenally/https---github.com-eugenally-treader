package com.edu.bootstring.invoice;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.order.SalesOrder;
import com.edu.bootstring.order.SalesOrderItem;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import com.edu.bootstring.quotation.Quotation;
import com.edu.bootstring.quotation.QuotationItem;
import com.edu.bootstring.quotation.QuotationRepository;
import com.edu.bootstring.shipment.Shipment;
import com.edu.bootstring.shipment.ShipmentItem;
import com.edu.bootstring.shipment.ShipmentRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인보이스 PDF 생성.
 *
 * <p>출력물은 D8-01 결정대로 영문 양식이다. 국제 표준 서식이기도 하고,
 * 덕분에 한글 폰트를 PDF 에 임베드할 필요가 없어 배포가 단순해진다.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final QuotationRepository quotationRepository;
    private final ShipmentRepository shipmentRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    /** 한 줄짜리 명세 라인 */
    private record Line(String code, String description, BigDecimal qty, String unit,
                        BigDecimal unitPrice, BigDecimal amount) {
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("인보이스를 찾을 수 없습니다. id=" + invoiceId));

        String html = buildHtml(invoice);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("PDF 생성에 실패했습니다: " + e.getMessage(),
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public String buildFileName(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(i -> i.getInvoiceNo() + ".pdf")
                .orElse("invoice.pdf");
    }

    private String buildHtml(Invoice invoice) {
        SalesOrder order = invoice.getOrderId() == null
                ? null
                : salesOrderRepository.findById(invoice.getOrderId()).orElse(null);
        Quotation quotation = invoice.getQuotationId() == null
                ? null
                : quotationRepository.findById(invoice.getQuotationId()).orElse(null);
        Shipment shipment = invoice.getShipmentId() == null
                ? null
                : shipmentRepository.findById(invoice.getShipmentId()).orElse(null);

        Long customerId = order != null ? order.getCustomerId()
                : quotation != null ? quotation.getCustomerId() : null;
        Customer customer = customerId == null ? null
                : customerRepository.findById(customerId).orElse(null);

        List<Line> lines = buildLines(invoice, order, quotation, shipment);

        boolean isCi = "CI".equals(invoice.getInvoiceType());
        String title = isCi ? "COMMERCIAL INVOICE" : "PROFORMA INVOICE";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\" /><style>");
        sb.append("""
                @page { size: A4; margin: 18mm 16mm; }
                body { font-family: Helvetica, Arial, sans-serif; font-size: 9.5pt; color: #111; }
                h1 { font-size: 16pt; letter-spacing: 2px; margin: 0 0 2mm 0; text-align: center; }
                .sub { text-align: center; font-size: 8pt; color: #666; margin-bottom: 6mm; }
                table { width: 100%; border-collapse: collapse; }
                .meta td { vertical-align: top; padding: 0 0 4mm 0; }
                .box { border: 1px solid #999; padding: 2.5mm; }
                .label { font-size: 7.5pt; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
                .items { margin-top: 4mm; }
                .items th { background: #eee; border: 1px solid #999; padding: 2mm; font-size: 8pt;
                            text-transform: uppercase; }
                .items td { border: 1px solid #999; padding: 2mm; }
                .r { text-align: right; }
                .c { text-align: center; }
                .total td { font-weight: bold; background: #f5f5f5; }
                .foot { margin-top: 8mm; font-size: 8pt; color: #444; }
                .sign { margin-top: 14mm; border-top: 1px solid #999; width: 55mm; padding-top: 1.5mm;
                        font-size: 8pt; }
                """);
        sb.append("</style></head><body>");

        sb.append("<h1>").append(title).append("</h1>");
        sb.append("<div class=\"sub\">TREADER CO., LTD. &#183; Seoul, Republic of Korea</div>");

        // 헤더 — 수신처 / 문서정보
        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td width=\"55%\"><div class=\"box\">");
        sb.append("<div class=\"label\">Messrs (Buyer)</div>");
        sb.append("<div><strong>").append(esc(customer == null ? "-" : customer.getNameEn()))
                .append("</strong></div>");
        if (customer != null && customer.getAddress() != null) {
            sb.append("<div>").append(esc(customer.getAddress())).append("</div>");
        }
        if (customer != null) {
            sb.append("<div>").append(esc(customer.getCountryCode())).append("</div>");
        }
        sb.append("</div></td>");

        sb.append("<td width=\"3%\"></td><td width=\"42%\"><div class=\"box\">");
        row(sb, "Invoice No.", invoice.getInvoiceNo());
        row(sb, "Issue Date", fmt(invoice.getIssueDate()));
        row(sb, "Due Date", fmt(invoice.getDueDate()));
        if (order != null) {
            row(sb, "Order No.", order.getOrderNo());
            row(sb, "Incoterms", order.getIncoterms());
        } else if (quotation != null) {
            row(sb, "Quotation No.", quotation.getQuoteNo());
            row(sb, "Incoterms", quotation.getIncoterms());
        }
        sb.append("</div></td></tr></table>");

        // 선적 정보 (CI 만)
        if (isCi && shipment != null) {
            sb.append("<table class=\"meta\"><tr><td><div class=\"box\">");
            sb.append("<table><tr>");
            cell(sb, "Vessel / Flight", shipment.getVesselName() != null
                    ? shipment.getVesselName() : shipment.getFlightNo());
            cell(sb, "B/L No.", shipment.getBlNo() != null ? shipment.getBlNo() : shipment.getAwbNo());
            cell(sb, "ETD", fmt(shipment.getEtd()));
            sb.append("</tr><tr>");
            cell(sb, "Port of Loading", shipment.getPortOfLoading());
            cell(sb, "Port of Discharge", shipment.getPortOfDischarge());
            cell(sb, "Container", shipment.getContainerType());
            sb.append("</tr></table>");
            sb.append("</div></td></tr></table>");
        }

        // 명세
        sb.append("<table class=\"items\"><tr>");
        sb.append("<th width=\"7%\">No</th><th width=\"18%\">Code</th><th>Description</th>");
        sb.append("<th width=\"13%\">Qty</th><th width=\"15%\">Unit Price</th><th width=\"17%\">Amount</th>");
        sb.append("</tr>");

        int no = 1;
        for (Line line : lines) {
            sb.append("<tr>");
            sb.append("<td class=\"c\">").append(no++).append("</td>");
            sb.append("<td>").append(esc(line.code())).append("</td>");
            sb.append("<td>").append(esc(line.description())).append("</td>");
            sb.append("<td class=\"r\">").append(num(line.qty(), 0)).append(' ')
                    .append(esc(line.unit())).append("</td>");
            sb.append("<td class=\"r\">").append(num(line.unitPrice(), 2)).append("</td>");
            sb.append("<td class=\"r\">").append(num(line.amount(), 2)).append("</td>");
            sb.append("</tr>");
        }

        sb.append("<tr class=\"total\"><td colspan=\"5\" class=\"r\">TOTAL (")
                .append(esc(invoice.getCurrency())).append(")</td><td class=\"r\">")
                .append(num(invoice.getAmount(), 2)).append("</td></tr>");

        if (invoice.getKrwAmount() != null) {
            sb.append("<tr><td colspan=\"5\" class=\"r\">Equivalent (KRW @ ")
                    .append(num(invoice.getExchangeRate(), 2)).append(", based on ")
                    .append(fmt(invoice.getRateBaseDate())).append(")</td><td class=\"r\">")
                    .append(num(invoice.getKrwAmount(), 0)).append("</td></tr>");
        }
        sb.append("</table>");

        // 결제 안내
        sb.append("<div class=\"foot\">");
        sb.append("<div><strong>Payment Terms:</strong> ")
                .append(esc(nvl(invoice.getPaymentTermsCode(), "-"))).append("</div>");
        sb.append("<div><strong>Amount Paid:</strong> ").append(esc(invoice.getCurrency())).append(' ')
                .append(num(invoice.getPaidAmount(), 2))
                .append(" &#183; <strong>Balance Due:</strong> ").append(esc(invoice.getCurrency()))
                .append(' ').append(num(invoice.getBalance(), 2)).append("</div>");
        if (isCi) {
            sb.append("<div>Payment is due within the credit period counted from the B/L date.</div>");
        } else {
            sb.append("<div>This is a proforma invoice for advance payment. ")
                    .append("Goods will be arranged upon receipt of the remittance.</div>");
        }
        sb.append("</div>");

        sb.append("<div class=\"sign\">Authorized Signature</div>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private List<Line> buildLines(Invoice invoice, SalesOrder order,
                                  Quotation quotation, Shipment shipment) {
        Map<Long, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Line> lines = new ArrayList<>();

        if (shipment != null && order != null) {
            // CI — 이번에 실제로 실은 수량만 청구한다
            Map<Long, SalesOrderItem> orderItems = order.getItems().stream()
                    .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

            for (ShipmentItem si : shipment.getItems()) {
                SalesOrderItem oi = orderItems.get(si.getOrderItemId());
                Product p = products.get(si.getProductId());
                BigDecimal unitPrice = oi != null ? oi.getUnitPrice() : BigDecimal.ZERO;
                lines.add(new Line(
                        p != null ? p.getProductCode() : "-",
                        describe(p),
                        si.getQty(),
                        p != null ? p.getUnit() : "",
                        unitPrice,
                        si.getQty().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)));
            }
            return lines;
        }

        if (quotation != null) {
            // 견적 단계 PI — 선금이므로 명세는 견적 전체를 싣고 금액만 비율 적용된 총액을 쓴다
            for (QuotationItem qi : quotation.getItems()) {
                Product p = products.get(qi.getProductId());
                lines.add(new Line(
                        p != null ? p.getProductCode() : "-",
                        describe(p),
                        qi.getQty(),
                        p != null ? p.getUnit() : "",
                        qi.getUnitPrice(),
                        qi.getAmount()));
            }
            return lines;
        }

        if (order != null) {
            for (SalesOrderItem oi : order.getItems()) {
                Product p = products.get(oi.getProductId());
                lines.add(new Line(
                        p != null ? p.getProductCode() : "-",
                        describe(p),
                        oi.getOrderedQty(),
                        p != null ? p.getUnit() : "",
                        oi.getUnitPrice(),
                        oi.getAmount()));
            }
        }
        return lines;
    }

    private String describe(Product p) {
        if (p == null) {
            return "-";
        }
        return p.getSpec() == null || p.getSpec().isBlank()
                ? p.getNameEn()
                : p.getNameEn() + " (" + p.getSpec() + ")";
    }

    private void row(StringBuilder sb, String label, String value) {
        sb.append("<div><span class=\"label\">").append(esc(label)).append(":</span> ")
                .append(esc(nvl(value, "-"))).append("</div>");
    }

    private void cell(StringBuilder sb, String label, String value) {
        sb.append("<td><div class=\"label\">").append(esc(label)).append("</div><div>")
                .append(esc(nvl(value, "-"))).append("</div></td>");
    }

    private String fmt(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String num(BigDecimal value, int scale) {
        if (value == null) {
            return "-";
        }
        return String.format("%,." + scale + "f", value.setScale(scale, RoundingMode.HALF_UP));
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** XHTML 로 렌더되므로 사용자 입력값은 반드시 이스케이프해야 파서가 깨지지 않는다 */
    private String esc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
