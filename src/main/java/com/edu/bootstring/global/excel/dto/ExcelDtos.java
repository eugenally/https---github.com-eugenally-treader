package com.edu.bootstring.global.excel.dto;

import java.util.List;

/**
 * 엑셀 대량등록 결과.
 */
public final class ExcelDtos {

    private ExcelDtos() {
    }

    /** 실패한 행 하나 — 엑셀 화면의 행 번호와 사유를 함께 준다 */
    public record RowError(
            int rowNum,
            String key,
            String reason
    ) {
    }

    /**
     * 업로드 결과.
     *
     * <p>부분 성공을 인정한다. 100건 중 3건이 틀렸다고 97건을 되돌리면
     * 사용자는 파일을 고쳐 처음부터 다시 올려야 한다. 성공한 건 넣고,
     * 실패한 건 <b>행 번호와 사유</b>를 돌려줘 그 행만 고치게 한다.
     */
    public record ImportResult(
            int totalRows,
            int successCount,
            int failCount,
            /** 이미 있어서 건너뛴 건수 */
            int skippedCount,
            List<RowError> errors
    ) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
