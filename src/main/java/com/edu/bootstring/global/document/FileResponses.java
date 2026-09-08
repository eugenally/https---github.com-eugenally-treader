package com.edu.bootstring.global.document;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * 파일 다운로드 응답을 만든다.
 *
 * <p>한글 파일명은 반드시 UTF-8 로 인코딩해야 한다. 그냥 넣으면
 * 브라우저에서 깨지거나 "다운로드"라는 이름으로 저장된다.
 */
public final class FileResponses {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private FileResponses() {
    }

    public static ResponseEntity<byte[]> pdf(byte[] content, String fileName) {
        return attachment(content, fileName, MediaType.APPLICATION_PDF);
    }

    public static ResponseEntity<byte[]> excel(byte[] content, String fileName) {
        return attachment(content, fileName, XLSX);
    }

    public static ResponseEntity<byte[]> attachment(byte[] content, String fileName, MediaType type) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(type)
                .body(content);
    }
}
