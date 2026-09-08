package com.edu.bootstring.attachment;

import com.edu.bootstring.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 업무 문서(견적·수주·출하·인보이스) 첨부파일.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public record AttachmentResponse(
            Long id,
            String refType,
            Long refId,
            String docType,
            String origName,
            String contentType,
            long fileSize,
            String createdBy,
            LocalDateTime createdAt,
            String downloadUrl
    ) {
        static AttachmentResponse of(Attachment a) {
            return new AttachmentResponse(
                    a.getId(), a.getRefType(), a.getRefId(), a.getDocType(),
                    a.getOrigName(), a.getContentType(), a.getFileSize(),
                    a.getCreatedBy(), a.getCreatedAt(), "/api/attachments/" + a.getId());
        }
    }

    /** 특정 문서에 붙은 첨부 목록 */
    @GetMapping
    public List<AttachmentResponse> findAll(@RequestParam String refType, @RequestParam Long refId) {
        return attachmentService.findAll(refType, refId).stream()
                .map(AttachmentResponse::of)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<AttachmentResponse> upload(@RequestParam String refType,
                                           @RequestParam Long refId,
                                           @RequestParam(required = false) String docType,
                                           @RequestPart("files") List<MultipartFile> files) {
        return attachmentService.upload(refType, refId, docType, files, MemberPrincipal.current())
                .stream()
                .map(AttachmentResponse::of)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.get(id);
        Resource resource = attachmentService.resolveResource(attachment);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getOrigName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(attachment.getContentType() != null
                        ? MediaType.parseMediaType(attachment.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
