package com.edu.bootstring.global.excel;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * 목록·양식을 xlsx 로 내보낸다.
 */
public final class ExcelWriter {

    private ExcelWriter() {
    }

    /** 한 열의 정의 — 머리글과, 데이터 객체에서 값을 꺼내는 방법 */
    public record Column<T>(String header, Function<T, Object> extractor, int width) {

        public static <T> Column<T> of(String header, Function<T, Object> extractor) {
            return new Column<>(header, extractor, 16);
        }

        public static <T> Column<T> of(String header, Function<T, Object> extractor, int width) {
            return new Column<>(header, extractor, width);
        }
    }

    public static <T> byte[] write(String sheetName, List<Column<T>> columns, List<T> rows) {
        return write(sheetName, columns, rows, null);
    }

    /**
     * @param guide 헤더 위에 한 줄 안내를 넣고 싶을 때 (업로드 양식에서 쓴다)
     */
    public static <T> byte[] write(String sheetName, List<Column<T>> columns, List<T> rows, String guide) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle guideStyle = guideStyle(workbook);

            int rowIdx = 0;
            if (guide != null) {
                Row guideRow = sheet.createRow(rowIdx++);
                Cell cell = guideRow.createCell(0);
                cell.setCellValue(guide);
                cell.setCellStyle(guideStyle);
            }

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).header());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, columns.get(i).width() * 256);
            }

            for (T row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.size(); i++) {
                    setValue(dataRow.createCell(i), columns.get(i).extractor().apply(row));
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("엑셀 생성에 실패했습니다: " + e.getMessage(),
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /** 숫자는 숫자 셀로 넣어야 엑셀에서 합계·정렬이 된다 */
    private static void setValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof BigDecimal b) {
            cell.setCellValue(b.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d.toString());
        } else if (value instanceof LocalDateTime dt) {
            cell.setCellValue(dt.toLocalDate().toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle guideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        return style;
    }
}
