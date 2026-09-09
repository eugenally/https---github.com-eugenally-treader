package com.edu.bootstring.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업무 문서 첨부파일 — <b>다형 참조</b>.
 *
 * <p>{@code refType}(QUOTATION/SALES_ORDER/SHIPMENT/INVOICE) + {@code refId} 조합으로
 * 네 종류의 문서를 한 테이블이 받는다. FK 를 걸 수 없고 JPA 연관관계도 못 만들지만,
 * 테이블 네 개를 만드는 것보다 낫다는 판단이다.
 *
 * <p><b>대가</b>: 무결성을 DB 가 아니라 애플리케이션이 책임진다.
 * 존재하지 않는 문서에 파일이 붙는 것을 막을 제약이 없으므로
 * {@link AttachmentService} 가 저장 전에 참조 대상을 반드시 확인한다.
 */
@Entity
@Getter
@Table(name = "ATTACHMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ATTACHMENT_ID")
    private Long id;

    /** DDL 의 CK_ATTACHMENT_REF 가 값을 네 가지로 제한한다 */
    @Column(name = "REF_TYPE", nullable = false, length = 20, updatable = false)
    private String refType;

    @Column(name = "REF_ID", nullable = false, updatable = false)
    private Long refId;

    /** 서류 종류 표시용 (PI/CI/PL/PO 등). 분류·검색에 쓴다. */
    @Column(name = "DOC_TYPE", length = 20)
    private String docType;

    @Column(name = "ORIG_NAME", nullable = false, length = 255)
    private String origName;

    @Column(name = "STORED_NAME", nullable = false, length = 255)
    private String storedName;

    @Column(name = "FILE_PATH", nullable = false, length = 500)
    private String filePath;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Attachment(String refType, Long refId, String docType, String origName,
                       String storedName, String filePath, String contentType,
                       Long fileSize, String createdBy) {
        this.refType = refType;
        this.refId = refId;
        this.docType = docType;
        this.origName = origName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
}
