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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시판 마스터.
 *
 * <p>자유·Q&A·자료실을 각각 테이블로 만들면 CRUD 코드를 세 벌 쓰게 된다.
 * 게시판을 <b>데이터로</b> 두면 서비스와 컨트롤러가 한 벌로 끝나고,
 * "공지사항 하나 더"가 INSERT 한 줄이 된다.
 *
 * <p>권한·첨부·댓글·좋아요 허용 여부를 전부 이 행이 결정한다.
 */
@Entity
@Getter
@Table(name = "BOARD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqBoard")
    @SequenceGenerator(name = "seqBoard", sequenceName = "SEQ_BOARD", allocationSize = 1)
    @Column(name = "BOARD_ID")
    private Long id;

    /** FREE / QNA / ARCHIVE */
    @Column(name = "BOARD_CODE", nullable = false, length = 20, updatable = false)
    private String boardCode;

    @Column(name = "BOARD_NAME", nullable = false, length = 50)
    private String boardName;

    /** ALL / MEMBER / STAFF */
    @Column(name = "AUTH_READ", nullable = false, length = 20)
    private String authRead;

    @Column(name = "AUTH_WRITE", nullable = false, length = 20)
    private String authWrite;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "FILE_YN", nullable = false, length = 1)
    private boolean fileAllowed;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "COMMENT_YN", nullable = false, length = 1)
    private boolean commentAllowed;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "LIKE_YN", nullable = false, length = 1)
    private boolean likeAllowed;

    @Column(name = "PAGE_SIZE", nullable = false)
    private Integer pageSize;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    /** 비회원도 글을 쓸 수 있는 게시판인가 */
    public boolean allowsGuestWrite() {
        return "ALL".equals(authWrite);
    }

    public boolean requiresLoginToRead() {
        return !"ALL".equals(authRead);
    }

    /** STAFF 는 ADMIN·SALES 만 쓸 수 있다는 뜻이다 (공지 등) */
    public boolean isStaffOnlyWrite() {
        return "STAFF".equals(authWrite);
    }
}
