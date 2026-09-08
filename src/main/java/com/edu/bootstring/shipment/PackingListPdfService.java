package com.edu.bootstring.shipment;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.document.PdfRenderer;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.order.SalesOrder;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Packing List PDF.
 *
 * <p>인보이스가 <b>돈</b>을 말한다면 PL 은 <b>물건</b>을 말한다.
 * 단가·금액은 넣지 않고 수량·포장·중량·부피만 싣는다.
 * 통관과 창고 검수에서 쓰는 서류라 금액이 들어가면 오히려 문제가 된다.
 */
@Service
@RequiredArgsConstructor
public class PackingListPdfService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PdfRenderer pdf;

    @Transactional(readOnly = true)
    public byte[] render(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new NotFoundException("출하를 찾을 수 없습니다. id=" + shipmentId));
        SalesOrder order = salesOrderRepository.findById(shipment.getOrderId()).orElse(null);
        Customer customer = order == null ? null
                : customerRepository.findById(order.getCustomerId()).orElse(null);
        Map<Long, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        boolean air = "AIR".equals(shipment.getTransportMode());

        StringBuilder sb = pdf.openDocument("PACKING LIST");

        sb.append("<table class=\"meta\"><tr>");
        sb.append("<td width=\"55%\"><div class=\"box\">");
        sb.append("<div class=\"label\">Messrs (Consignee)</div>");
        sb.append("<div><strong>")
                .append(pdf.escape(customer == null ? "-" : customer.getNameEn()))
                .append("</strong></div>");
        if (customer != null && customer.getAddress() != null) {
            sb.append("<div>").append(pdf.escape(customer.getAddress())).append("</div>");
        }
        sb.append("</div></td>");

        sb.append("<td width=\"3%\"></td><td width=\"42%\"><div class=\"box\">");
        pdf.labelledLine(sb, "Packing List No.", shipment.getShipmentNo());
        pdf.labelledLine(sb, "Order No.", order != null ? order.getOrderNo() : null);
        pdf.labelledLine(sb, "Ship Date", pdf.date(shipment.getShipDate()));
        pdf.labelledLine(sb, "Incoterms", order != null ? order.getIncoterms() : null);
        sb.append("</div></td></tr></table>");

        // 운송 정보 — 해상/항공에 따라 필드가 바뀐다 (D8-02)
        sb.append("<table class=\"meta\"><tr><td><div class=\"box\"><table><tr>");
        if (air) {
            pdf.labelledCell(sb, "Flight No.", shipment.getFlightNo());
            pdf.labelledCell(sb, "AWB No.", shipment.getAwbNo());
        } else {
            pdf.labelledCell(sb, "Vessel", shipment.getVesselName());
            pdf.labelledCell(sb, "B/L No.", shipment.getBlNo());
        }
        pdf.labelledCell(sb, "ETD", pdf.date(shipment.getEtd()));
        sb.append("</tr><tr>");
        pdf.labelledCell(sb, "Port of Loading", shipment.getPortOfLoading());
        pdf.labelledCell(sb, "Port of Discharge", shipment.getPortOfDischarge());
        pdf.labelledCell(sb, air ? "Airline" : "Container",
                air ? shipment.getVesselName() : shipment.getContainerType());
        sb.append("</tr></table></div></td></tr></table>");

        // 명세 — 금액은 넣지 않는다
        sb.append("<table class=\"items\"><tr>");
        sb.append("<th width=\"7%\">No</th><th width=\"18%\">Code</th><th>Description</th>");
        sb.append("<th width=\"11%\">Qty</th><th width=\"10%\">Ctns</th>");
        sb.append("<th width=\"12%\">N/W (kg)</th><th width=\"12%\">G/W (kg)</th>");
        sb.append("</tr>");

        int no = 1;
        for (ShipmentItem item : shipment.getItems()) {
            Product p = products.get(item.getProductId());
            sb.append("<tr>");
            sb.append("<td class=\"c\">").append(no++).append("</td>");
            sb.append("<td>").append(pdf.escape(p != null ? p.getProductCode() : "-")).append("</td>");
            sb.append("<td>").append(pdf.escape(describe(p))).append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getQty(), 0)).append(' ')
                    .append(pdf.escape(p != null ? p.getUnit() : "")).append("</td>");
            sb.append("<td class=\"r\">")
                    .append(item.getCartonQty() != null ? item.getCartonQty() : "-").append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getNetWeight(), 2)).append("</td>");
            sb.append("<td class=\"r\">").append(pdf.number(item.getGrossWeight(), 2)).append("</td>");
            sb.append("</tr>");
        }

        sb.append("<tr class=\"total\"><td colspan=\"3\" class=\"r\">TOTAL</td>");
        sb.append("<td class=\"r\">").append(pdf.number(sumQty(shipment), 0)).append("</td>");
        sb.append("<td class=\"r\">")
                .append(shipment.getTotalCarton() != null ? shipment.getTotalCarton() : "-").append("</td>");
        sb.append("<td class=\"r\">").append(pdf.number(shipment.getTotalNetWeight(), 2)).append("</td>");
        sb.append("<td class=\"r\">").append(pdf.number(shipment.getTotalGrossWeight(), 2)).append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<div class=\"foot\">");
        if (shipment.getTotalCbm() != null) {
            sb.append("<div><strong>Total Measurement:</strong> ")
                    .append(pdf.number(shipment.getTotalCbm(), 3)).append(" CBM</div>");
        }
        sb.append("<div><strong>Shipping Marks:</strong> ")
                .append(pdf.escape(customer != null ? customer.getCustomerCode() : "N/M"))
                .append(" / ").append(pdf.escape(shipment.getShipmentNo()))
                .append(" / MADE IN KOREA</div>");
        sb.append("<div style=\"margin-top:2mm\">We hereby certify that the above particulars are ")
                .append("true and correct.</div>");
        sb.append("</div>");

        pdf.closeDocument(sb);
        return pdf.render(sb.toString());
    }

    @Transactional(readOnly = true)
    public String fileName(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .map(s -> "PL-" + s.getShipmentNo() + ".pdf")
                .orElse("packing-list.pdf");
    }

    private BigDecimal sumQty(Shipment shipment) {
        return shipment.getItems().stream()
                .map(ShipmentItem::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
