package com.edu.bootstring.global.excel;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 업로드된 xlsx 를 행 단위로 읽어 준다.
 *
 * <p>셀 타입이 제각각인 게 엑셀 업로드의 실제 어려움이다.
 * 사용자가 수량을 텍스트로 넣거나 코드 앞에 공백을 붙이는 일이 늘 생기므로
 * 여기서 타입을 흡수하고, 값 변환 실패는 <b>행 단위 오류</b>로 넘긴다.
 */
public final class ExcelReader {

    private ExcelReader() {
    }

    /** 한 행을 헤더명 → 문자열 값으로 담은 것. 행 번호는 사용자에게 보여줄 1-based 값이다. */
    public record RowData(int rowNum, Map<String, String> values) {

        public String str(String header) {
            String v = values.get(header);
            return (v == null || v.isBlank()) ? null : v.trim();
        }

        public String required(String header) {
            String v = str(header);
            if (v == null) {
                throw new IllegalArgumentException(header + " 은(는) 필수입니다.");
            }
            return v;
        }

        public BigDecimal decimal(String header) {
            String v = str(header);
            if (v == null) {
                return null;
            }
            try {
                return new BigDecimal(v.replace(",", ""));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("%s 은(는) 숫자여야 합니다: %s".formatted(header, v));
            }
        }

        public Integer integer(String header) {
            BigDecimal v = decimal(header);
            return v == null ? null : v.intValue();
        }

        public LocalDate date(String header) {
            String v = str(header);
            if (v == null) {
                return null;
            }
            try {
                return LocalDate.parse(v);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "%s 은(는) yyyy-MM-dd 형식이어야 합니다: %s".formatted(header, v));
            }
        }

        public LocalDate requiredDate(String header) {
            LocalDate v = date(header);
            if (v == null) {
                throw new IllegalArgumentException(header + " 은(는) 필수입니다.");
            }
            return v;
        }

        /** 행 전체가 비었는지 — 엑셀 하단의 빈 행을 건너뛰는 데 쓴다 */
        public boolean isEmpty() {
            return values.values().stream().allMatch(v -> v == null || v.isBlank());
        }
    }

    /** 헤더를 찾기 위해 훑어볼 최대 행 수 — 안내문·빈 줄이 몇 개 있어도 견딘다 */
    private static final int HEADER_SCAN_LIMIT = 10;

    /**
     * 첫 시트를 읽는다.
     *
     * <p>헤더 행을 <b>위치가 아니라 내용으로</b> 찾는다. 양식 맨 위에 안내문 한 줄이 있고,
     * 사용자가 빈 줄을 더 넣기도 하기 때문이다. 첫 행을 헤더로 단정하면 그 순간 깨진다.
     *
     * <p>{@code requiredHeaders} 를 모두 가진 행을 찾지 못하면 데이터를 한 줄도 읽지 않고
     * 즉시 거절한다 — 엉뚱한 양식을 올린 경우다.
     */
    public static List<RowData> read(MultipartFile file, List<String> requiredHeaders) {
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("시트가 없는 파일입니다.", ErrorCode.INVALID_INPUT_VALUE);
            }

            Row headerRow = null;
            Map<Integer, String> headers = Map.of();
            int scanEnd = Math.min(sheet.getFirstRowNum() + HEADER_SCAN_LIMIT, sheet.getLastRowNum());

            for (int i = sheet.getFirstRowNum(); i <= scanEnd; i++) {
                Row candidate = sheet.getRow(i);
                if (candidate == null) {
                    continue;
                }
                Map<Integer, String> found = readHeaderNames(candidate);
                if (found.values().containsAll(requiredHeaders)) {
                    headerRow = candidate;
                    headers = found;
                    break;
                }
            }

            if (headerRow == null) {
                throw new BusinessException(
                        "양식이 맞지 않습니다. 다음 열이 모두 있는 행을 찾지 못했습니다: %s. 양식 다운로드를 이용해 주세요."
                                .formatted(String.join(", ", requiredHeaders)),
                        ErrorCode.INVALID_INPUT_VALUE);
            }

            List<RowData> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                headers.forEach((idx, name) -> values.put(name, stringValue(row.getCell(idx))));

                RowData data = new RowData(i + 1, values);   // 엑셀 화면의 행 번호와 맞춘다
                if (!data.isEmpty()) {
                    rows.add(data);
                }
            }
            return rows;

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage(),
                    ErrorCode.INVALID_INPUT_VALUE);
        } catch (RuntimeException e) {
            // POI 는 xlsx 가 아닌 파일에 NotOfficeXmlFileException 을 던진다.
            // IOException 이 아니라 RuntimeException 계열이라 위에서 안 잡히고 500 으로 새어 나갔다.
            // 잘못된 파일을 올린 건 클라이언트 잘못이므로 400 으로 내려야 한다.
            throw new BusinessException(
                    "엑셀(.xlsx) 파일이 아니거나 형식이 손상되었습니다. 양식 다운로드를 이용해 주세요.",
                    ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /** 한 행을 열번호 → 머리글 이름으로 읽는다. 빈 칸은 건너뛴다. */
    private static Map<Integer, String> readHeaderNames(Row row) {
        Map<Integer, String> names = new HashMap<>();
        for (Cell cell : row) {
            String name = stringValue(cell);
            if (name != null && !name.isBlank()) {
                names.put(cell.getColumnIndex(), name.trim());
            }
        }
        return names;
    }

    /**
     * 셀 값을 문자열로 뽑는다.
     *
     * <p>숫자 셀이 {@code 1.0} 으로 읽히면 제품코드가 "1.0" 이 되어 버리므로
     * 정수는 소수점을 떼고, 날짜는 ISO 로 바꾼다.
     */
    private static String stringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) && !Double.isInfinite(d)
                        ? String.valueOf((long) d)
                        : BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            }
            case FORMULA -> formulaValue(cell);
            default -> null;
        };
    }

    private static String formulaValue(Cell cell) {
        try {
            return cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                    : cell.getStringCellValue();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
