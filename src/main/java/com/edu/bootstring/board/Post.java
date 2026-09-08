package com.edu.bootstring.board;

import com.edu.bootstring.global.common.BaseTimeEntity;
import com.edu.bootstring.global.common.YesNoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글.
 *
 * <p>{@code memberId} 가 비어 있으면 비회원 글이고, 이때는 {@code guestName}/{@code guestPwd} 를 쓴다.
 * DDL 의 CK_POST_WRITER 제약이 둘 중 하나는 반드시 채워지도록 강제한다.
 *
 * <p>{@code viewCnt}·{@code likeCnt}·{@code commentCnt} 는 비정규화 컬럼이다.
 * 목록에서 매번 {@code COUNT(*)} 를 돌리면 느리다. 진실의 원천은 각 자식 테이블이고
 * 이 값들은 파생값이므로, 갱신은 반드시 같은 트랜잭션 안에서 해야 한다.
 */
@Entity
@Getter
@Table(name = "POST")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPost")
    @SequenceGenerator(name = "seqPost", sequenceName = "SEQ_POST", allocationSize = 1)
    @Column(name = "POST_ID")
    private Long id;

    @Column(name = "BOARD_ID", nullable = false, updatable = false)
    private Long boardId;

    /** NULL 이면 비회원 글 */
    @Column(name = "MEMBER_ID", updatable = false)
    private Long memberId;

    @Column(name = "GUEST_NAME", length = 50)
    private String guestName;

    /** 비회원 글의 수정·삭제 확인용 BCrypt 해시 */
    @Column(name = "GUEST_PWD", length = 100)
    private String guestPwd;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "VIEW_CNT", nullable = false)
    private int viewCnt;

    @Column(name = "LIKE_CNT", nullable = false)
    private int likeCnt;

    @Column(name = "COMMENT_CNT", nullable = false)
    private int commentCnt;

    /** Q&A 답변 완료 표시 */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "ANSWERED_YN", nullable = false, length = 1)
    private boolean answered;

    /**
     * 소프트 삭제. 댓글이 달린 글을 물리 삭제하면 참조가 깨진다.
     * 목록·조회에서 {@code delYn='N'} 으로 거른다.
     */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "DEL_YN", nullable = false, length = 1)
    private boolean deleted;

    @Column(name = "WRITER_IP", length = 45)
    private String writerIp;

    // 설계 노트는 "게시글 수정 = 낙관적 락"을 적어 두었지만 POST 테이블에는 VERSION 컬럼이 없다.
    // 스키마(oracle_ddl.sql)가 원본이라는 결정을 따라 여기서는 락을 걸지 않는다.
    // 넣으려면 ALTER TABLE POST ADD VERSION NUMBER(19) DEFAULT 0 NOT NULL 이 먼저다.

    @Builder
    private Post(Long boardId, Long memberId, String guestName, String guestPwd,
                 String title, String content, String writerIp) {
        this.boardId = boardId;
        this.memberId = memberId;
        this.guestName = guestName;
        this.guestPwd = guestPwd;
        this.title = title;
        this.content = content;
        this.writerIp = writerIp;
        this.viewCnt = 0;
        this.likeCnt = 0;
        this.commentCnt = 0;
        this.answered = false;
        this.deleted = false;
    }

    public boolean isGuestPost() {
        return memberId == null;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void increaseViewCount() {
        this.viewCnt++;
    }

    public void applyLikeCount(int count) {
        this.likeCnt = count;
    }

    public void applyCommentCount(int count) {
        this.commentCnt = count;
    }

    public void markAnswered(boolean answered) {
        this.answered = answered;
    }

    /** 이 회원이 쓴 글인가 */
    public boolean isOwnedBy(Long memberId) {
        return this.memberId != null && this.memberId.equals(memberId);
    }
}
