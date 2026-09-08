package com.edu.bootstring.board;

import com.edu.bootstring.global.common.YesNoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * 댓글. {@code parentId} 로 한 단계 대댓글까지 표현한다.
 *
 * <p>댓글은 회원만 쓸 수 있다 (DDL 의 MEMBER_ID NOT NULL).
 * 삭제도 소프트 삭제다 — 대댓글이 달린 댓글을 지우면 트리가 끊긴다.
 */
@Entity
@Getter
@Table(name = "POST_COMMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqComment")
    @SequenceGenerator(name = "seqComment", sequenceName = "SEQ_COMMENT", allocationSize = 1)
    @Column(name = "COMMENT_ID")
    private Long id;

    @Column(name = "POST_ID", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "MEMBER_ID", nullable = false, updatable = false)
    private Long memberId;

    /** 대댓글이면 부모 댓글 ID */
    @Column(name = "PARENT_ID", updatable = false)
    private Long parentId;

    @Column(name = "CONTENT", nullable = false, length = 2000)
    private String content;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "DEL_YN", nullable = false, length = 1)
    private boolean deleted;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Builder
    private PostComment(Long postId, Long memberId, Long parentId, String content) {
        this.postId = postId;
        this.memberId = memberId;
        this.parentId = parentId;
        this.content = content;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
