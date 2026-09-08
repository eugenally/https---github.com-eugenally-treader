package com.edu.bootstring.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좋아요. (게시글, 회원) 복합키라 한 사람이 같은 글에 두 번 누를 수 없다.
 * 중복 방지를 애플리케이션이 아니라 <b>PK 제약</b>이 보장한다.
 */
@Entity
@Getter
@Table(name = "POST_LIKE")
@IdClass(PostLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @Column(name = "POST_ID")
    private Long postId;

    @Id
    @Column(name = "MEMBER_ID")
    private Long memberId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostLike(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }
}
