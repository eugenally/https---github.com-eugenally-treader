package com.edu.bootstring.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 조회.
 *
 * <p>검색 조건을 파라미터 하나로 분기하는 큰 HQL 을 쓰지 않고 메서드를 나눴다.
 * Hibernate 가 {@code lower()} 를 CLOB 인 {@code CONTENT} 에 적용하지 못해서이기도 하고,
 * 조건별로 나눠 두는 편이 읽기도 낫다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByBoardIdAndDeletedFalse(Long boardId, Pageable pageable);

    @Query("""
            select p from Post p
             where p.boardId = :boardId and p.deleted = false
               and lower(p.title) like lower(concat('%', :keyword, '%'))
            """)
    Page<Post> searchByTitle(@Param("boardId") Long boardId,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    /**
     * 본문 검색.
     *
     * <p>CONTENT 가 CLOB 이라 {@code lower()} 를 걸 수 없다. 그래서 <b>대소문자를 구분</b>한다.
     * 한글에는 대소문자가 없어 실사용에서는 거의 문제가 되지 않는다.
     * 영문까지 대소문자 무시로 찾아야 하면 Oracle Text 인덱스가 필요하다.
     */
    @Query("""
            select p from Post p
             where p.boardId = :boardId and p.deleted = false
               and p.content like concat('%', :keyword, '%')
            """)
    Page<Post> searchByContent(@Param("boardId") Long boardId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /** 작성자 — 회원 글은 CREATED_BY(로그인 아이디), 비회원 글은 GUEST_NAME 을 본다 */
    @Query("""
            select p from Post p
             where p.boardId = :boardId and p.deleted = false
               and (lower(p.guestName) like lower(concat('%', :keyword, '%'))
                 or lower(p.createdBy) like lower(concat('%', :keyword, '%')))
            """)
    Page<Post> searchByWriter(@Param("boardId") Long boardId,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    /** 기본 검색 — 제목 또는 본문 */
    @Query("""
            select p from Post p
             where p.boardId = :boardId and p.deleted = false
               and (lower(p.title) like lower(concat('%', :keyword, '%'))
                 or p.content like concat('%', :keyword, '%'))
            """)
    Page<Post> searchByTitleOrContent(@Param("boardId") Long boardId,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    Optional<Post> findByIdAndDeletedFalse(Long id);

    long countByBoardIdAndDeletedFalse(Long boardId);
}
