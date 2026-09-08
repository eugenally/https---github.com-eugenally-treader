package com.edu.bootstring.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /** 삭제된 댓글도 함께 준다. 대댓글이 달려 있으면 "삭제된 댓글입니다"로 자리를 지켜야 하기 때문이다. */
    List<PostComment> findAllByPostIdOrderByIdAsc(Long postId);

    int countByPostIdAndDeletedFalse(Long postId);

    boolean existsByParentIdAndDeletedFalse(Long parentId);
}
