package com.edu.bootstring.attachment;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;

import java.util.Arrays;

/**
 * 첨부가 붙을 수 있는 문서 종류. DDL 의 CK_ATTACHMENT_REF 와 값이 맞아야 한다.
 */
public enum AttachmentRefType {

    QUOTATION("견적"),
    SALES_ORDER("수주"),
    SHIPMENT("출하"),
    INVOICE("인보이스");

    private final String label;

    AttachmentRefType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AttachmentRefType of(String value) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "첨부할 수 없는 문서 종류입니다: " + value, ErrorCode.INVALID_INPUT_VALUE));
    }
}
