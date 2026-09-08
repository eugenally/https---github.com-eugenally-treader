package com.edu.bootstring.board;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.security.MemberPrincipal;
import org.springframework.stereotype.Component;

/**
 * 게시판 접근 규칙을 한 곳에 모은다.
 *
 * <p>규칙은 코드가 아니라 {@link Board} 행이 정한다. 새 게시판을 INSERT 로 추가해도
 * 이 클래스는 그대로 동작한다.
 */
@Component
public class BoardAccessPolicy {

    private static final String ALL = "ALL";
    private static final String MEMBER = "MEMBER";
    private static final String STAFF = "STAFF";

    /** 읽기 권한. 통과하지 못하면 예외를 던진다. */
    public void checkRead(Board board, MemberPrincipal principal) {
        if (!canRead(board, principal)) {
            throw new BusinessException(
                    "%s 은(는) 로그인 후 이용할 수 있습니다.".formatted(board.getBoardName()),
                    principal == null ? ErrorCode.UNAUTHORIZED : ErrorCode.ACCESS_DENIED);
        }
    }

    public boolean canRead(Board board, MemberPrincipal principal) {
        return switch (board.getAuthRead()) {
            case ALL -> true;
            case MEMBER -> principal != null;
            case STAFF -> isStaff(principal);
            default -> false;
        };
    }

    /** 쓰기 권한. 비회원 쓰기가 허용된 게시판이면 principal 이 없어도 통과한다. */
    public void checkWrite(Board board, MemberPrincipal principal) {
        if (!canWrite(board, principal)) {
            throw new BusinessException(
                    "%s 에 글을 쓸 권한이 없습니다.".formatted(board.getBoardName()),
                    principal == null ? ErrorCode.UNAUTHORIZED : ErrorCode.ACCESS_DENIED);
        }
    }

    public boolean canWrite(Board board, MemberPrincipal principal) {
        return switch (board.getAuthWrite()) {
            case ALL -> true;
            case MEMBER -> principal != null;
            case STAFF -> isStaff(principal);
            default -> false;
        };
    }

    /**
     * 글 수정·삭제 권한.
     *
     * <p>회원 글은 작성자 본인이나 관리자만 손댈 수 있다.
     * 비회원 글은 여기서 통과시키고, 비밀번호 확인을 호출한 쪽에서 따로 한다.
     */
    public boolean canModifyPost(Post post, MemberPrincipal principal) {
        if (isAdmin(principal)) {
            return true;
        }
        if (post.isGuestPost()) {
            return true;    // 비밀번호로 다시 확인한다
        }
        return principal != null && post.isOwnedBy(principal.memberId());
    }

    public boolean canModifyComment(PostComment comment, MemberPrincipal principal) {
        return isAdmin(principal) || (principal != null && comment.isOwnedBy(principal.memberId()));
    }

    /** 댓글은 게시판 설정이 허용해야 하고, 회원만 쓸 수 있다 (DDL 의 MEMBER_ID NOT NULL) */
    public void checkComment(Board board, MemberPrincipal principal) {
        if (!board.isCommentAllowed()) {
            throw new BusinessException(
                    "%s 은(는) 댓글을 지원하지 않습니다.".formatted(board.getBoardName()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }
        if (principal == null) {
            throw new BusinessException("댓글은 로그인 후 쓸 수 있습니다.", ErrorCode.UNAUTHORIZED);
        }
    }

    public void checkLike(Board board, MemberPrincipal principal) {
        if (!board.isLikeAllowed()) {
            throw new BusinessException(
                    "%s 은(는) 좋아요를 지원하지 않습니다.".formatted(board.getBoardName()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }
        if (principal == null) {
            throw new BusinessException("좋아요는 로그인 후 누를 수 있습니다.", ErrorCode.UNAUTHORIZED);
        }
    }

    public void checkFileUpload(Board board) {
        if (!board.isFileAllowed()) {
            throw new BusinessException(
                    "%s 은(는) 파일 첨부를 지원하지 않습니다.".formatted(board.getBoardName()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean isAdmin(MemberPrincipal principal) {
        return principal != null && "ADMIN".equals(principal.role());
    }

    private boolean isStaff(MemberPrincipal principal) {
        return principal != null
                && ("ADMIN".equals(principal.role()) || "SALES".equals(principal.role()));
    }
}
