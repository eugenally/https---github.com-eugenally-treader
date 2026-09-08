package com.edu.bootstring.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

    List<PostFile> findAllByPostIdOrderByIdAsc(Long postId);

    /**
     * 목록 화면의 첨부 개수. 글 하나씩 세면 N+1 이 되므로 한 번에 집계한다.
     * {@code [postId, count]} 배열로 돌아온다.
     */
    @Query("select f.postId, count(f) from PostFile f where f.postId in :postIds group by f.postId")
    List<Object[]> countGroupedByPostId(@Param("postIds") List<Long> postIds);
}
