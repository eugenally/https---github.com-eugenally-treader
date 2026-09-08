package com.edu.bootstring.board;

import java.io.Serializable;
import java.util.Objects;

/**
 * POST_LIKE 복합키
 */
public class PostLikeId implements Serializable {

    private Long postId;
    private Long memberId;

    public PostLikeId() {
    }

    public PostLikeId(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostLikeId other)) {
            return false;
        }
        return Objects.equals(postId, other.postId) && Objects.equals(memberId, other.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, memberId);
    }
}
