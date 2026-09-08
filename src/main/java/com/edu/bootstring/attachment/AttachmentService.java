package com.edu.bootstring.attachment;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.global.security.MemberPrincipal;
import com.edu.bootstring.invoice.InvoiceRepository;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.quotation.QuotationRepository;
import com.edu.bootstring.shipment.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 업무 문서 첨부파일.
 *
 * <p>다형 참조라 DB 가 무결성을 지켜 주지 못한다. 그래서 저장 전에
 * <b>참조 대상이 실제로 있는지 반드시 확인</b>한다. 이 확인이 곧 FK 를 대신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;   // 20MB

    private final AttachmentRepository attachmentRepository;
    private final QuotationRepository quotationRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ShipmentRepository shipmentRepository;
    private final InvoiceRepository invoiceRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<Attachment> findAll(String refType, Long refId) {
        AttachmentRefType type = AttachmentRefType.of(refType);
        requireReferenceExists(type, refId);
        return attachmentRepository.findAllByRefTypeAndRefIdOrderByIdAsc(type.name(), refId);
    }

    @Transactional
    public List<Attachment> upload(String refType, Long refId, String docType,
                                   List<MultipartFile> files, MemberPrincipal principal) {
        AttachmentRefType type = AttachmentRefType.of(refType);
        requireReferenceExists(type, refId);

        return files.stream()
                .filter(f -> !f.isEmpty())
                .map(f -> store(type, refId, docType, f, principal))
                .toList();
    }

    private Attachment store(AttachmentRefType type, Long refId, String docType,
                             MultipartFile file, MemberPrincipal principal) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    "파일 크기는 20MB 를 넘을 수 없습니다. (%s)".formatted(file.getOriginalFilename()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        String origName = sanitize(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + extensionOf(origName);
        String relativeDir = "docs/" + LocalDate.now().format(DATE_PATH);

        try {
            Path dir = Paths.get(uploadDir, relativeDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, dir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }

            return attachmentRepository.save(Attachment.builder()
                    .refType(type.name())
                    .refId(refId)
                    .docType(docType)
                    .origName(origName)
                    .storedName(storedName)
                    .filePath(relativeDir + "/" + storedName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .createdBy(principal != null ? principal.loginId() : null)
                    .build());

        } catch (IOException e) {
            log.error("첨부 저장 실패 name={} : {}", origName, e.getMessage());
            throw new BusinessException("파일 저장에 실패했습니다: " + origName,
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public Attachment get(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("첨부파일을 찾을 수 없습니다. id=" + id));
    }

    public Resource resolveResource(Attachment attachment) {
        try {
            Path path = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(attachment.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("파일이 저장소에 없습니다: " + attachment.getOrigName());
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException("파일을 읽을 수 없습니다: " + attachment.getOrigName(),
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void delete(Long id) {
        attachmentRepository.delete(get(id));
        // 디스크 파일은 남긴다. 지우다 실패하면 트랜잭션과 어긋나므로 정리는 배치의 몫이다.
    }

    /**
     * FK 를 대신하는 확인. 없는 문서에 파일이 붙으면 나중에 아무도 찾지 못하는 고아가 된다.
     */
    private void requireReferenceExists(AttachmentRefType type, Long refId) {
        boolean exists = switch (type) {
            case QUOTATION -> quotationRepository.existsById(refId);
            case SALES_ORDER -> salesOrderRepository.existsById(refId);
            case SHIPMENT -> shipmentRepository.existsById(refId);
            case INVOICE -> invoiceRepository.existsById(refId);
        };
        if (!exists) {
            throw new NotFoundException(
                    "%s 문서를 찾을 수 없습니다. id=%d".formatted(type.label(), refId));
        }
    }

    /** 경로 조작 방지 — {@code ../../etc/passwd} 같은 이름을 그대로 쓰면 저장 경로를 벗어난다 */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return Paths.get(name).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot > -1 ? name.substring(dot) : "";
    }
}
