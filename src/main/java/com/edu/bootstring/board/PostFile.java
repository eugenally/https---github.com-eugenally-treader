package com.edu.bootstring.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 첨부파일.
 *
 * <p>업무 문서 첨부({@code ATTACHMENT})와 왜 따로인가 — 게시판은 FK 를 정상적으로 걸 수 있고,
 * {@code mediaType} 에 따라 화면 렌더를 바꾸는 고유 요구가 있다. 성격이 달라 억지로 합치지 않았다.
 */
@Entity
@Getter
@Table(name = "POST_FILE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPostFile")
    @SequenceGenerator(name = "seqPostFile", sequenceName = "SEQ_POST_FILE", allocationSize = 1)
    @Column(name = "FILE_ID")
    private Long id;

    @Column(name = "POST_ID", nullable = false, updatable = false)
    private Long postId;

    /** 사용자가 올린 원래 이름. 다운로드 시 이 이름으로 돌려준다. */
    @Column(name = "ORIG_NAME", nullable = false, length = 255)
    private String origName;

    /** 디스크에 저장된 이름(UUID). 원본 이름을 그대로 쓰면 충돌·경로조작 위험이 있다. */
    @Column(name = "STORED_NAME", nullable = false, length = 255)
    private String storedName;

    @Column(name = "FILE_PATH", nullable = false, length = 500)
    private String filePath;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    /** IMAGE / VIDEO / AUDIO / OTHER — 화면에서 미리보기 방식을 가른다 */
    @Column(name = "MEDIA_TYPE", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Column(name = "DOWNLOAD_CNT", nullable = false)
    private int downloadCnt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PostFile(Long postId, String origName, String storedName, String filePath,
                     String contentType, String mediaType, Long fileSize) {
        this.postId = postId;
        this.origName = origName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
        this.downloadCnt = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void increaseDownloadCount() {
        this.downloadCnt++;
    }

    /** Content-Type 으로 미리보기 종류를 정한다. 없으면 OTHER 로 떨어뜨린다. */
    public static String resolveMediaType(String contentType) {
        if (contentType == null) {
            return "OTHER";
        }
        String lower = contentType.toLowerCase();
        if (lower.startsWith("image/")) {
            return "IMAGE";
        }
        if (lower.startsWith("video/")) {
            return "VIDEO";
        }
        if (lower.startsWith("audio/")) {
            return "AUDIO";
        }
        return "OTHER";
    }
}
