package com.edu.bootstring.global.document;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * HTML 을 PDF 로 렌더한다. 문서 세 종류(PI/CI, 견적서, Packing List)가 공유한다.
 *
 * <p>출력물은 D8-01 대로 <b>영문 양식</b>이다. 국제 표준 서식이기도 하고,
 * 덕분에 한글 폰트를 PDF 에 임베드할 필요가 없어 배포가 단순해진다.
 */
@Component
public class PdfRenderer {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /** 세 문서가 공유하는 스타일. 여기만 고치면 출력물 전체의 인상이 바뀐다. */
    public static final String BASE_CSS = """
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
            """;

    public byte[] render(String html) {
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

    /** 문서 머리 — 제목과 회사 표시 */
    public StringBuilder openDocument(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\" /><style>").append(BASE_CSS)
                .append("</style></head><body>");
        sb.append("<h1>").append(escape(title)).append("</h1>");
        sb.append("<div class=\"sub\">TREADER CO., LTD. &#183; Seoul, Republic of Korea</div>");
        return sb;
    }

    public void closeDocument(StringBuilder sb) {
        sb.append("<div class=\"sign\">Authorized Signature</div>");
        sb.append("</body></html>");
    }

    public void labelledLine(StringBuilder sb, String label, String value) {
        sb.append("<div><span class=\"label\">").append(escape(label)).append(":</span> ")
                .append(escape(nvl(value))).append("</div>");
    }

    public void labelledCell(StringBuilder sb, String label, String value) {
        sb.append("<td><div class=\"label\">").append(escape(label)).append("</div><div>")
                .append(escape(nvl(value))).append("</div></td>");
    }

    public String date(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    public String number(BigDecimal value, int scale) {
        if (value == null) {
            return "-";
        }
        return String.format("%,." + scale + "f", value.setScale(scale, RoundingMode.HALF_UP));
    }

    public String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * XHTML 로 렌더되므로 사용자 입력값은 반드시 이스케이프해야 한다.
     * 제품명에 {@code &} 하나만 들어가도 파서가 통째로 실패한다.
     */
    public String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
