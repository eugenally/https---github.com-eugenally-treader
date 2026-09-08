package com.edu.bootstring.invoice;

import com.edu.bootstring.invoice.dto.InvoiceDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @GetMapping
    public List<InvoiceDtos.Response> findAll() {
        return invoiceService.findAll();
    }

    @GetMapping("/{id}")
    public InvoiceDtos.Response findOne(@PathVariable Long id) {
        return invoiceService.findOne(id);
    }

    /** 확정된 출하에 대한 CI 발행 */
    @PostMapping("/ci")
    public InvoiceDtos.Response issueCi(@Valid @RequestBody InvoiceDtos.IssueCiRequest req) {
        return invoiceService.issueCommercialInvoice(req);
    }

    /** 견적 단계 선금 PI 발행 */
    @PostMapping("/pi")
    public InvoiceDtos.Response issuePi(@Valid @RequestBody InvoiceDtos.IssuePiRequest req) {
        return invoiceService.issueProformaInvoice(req);
    }

    @PostMapping("/{id}/payments")
    public InvoiceDtos.Response registerPayment(@PathVariable Long id,
                                                @Valid @RequestBody InvoiceDtos.PaymentRequest req) {
        return invoiceService.registerPayment(id, req);
    }

    /** 인보이스 PDF 다운로드 */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] pdf = invoicePdfService.render(id);
        String fileName = invoicePdfService.buildFileName(id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
