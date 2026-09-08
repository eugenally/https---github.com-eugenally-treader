package com.edu.bootstring.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시판 API 의 요청/응답 형태
 */
public final class BoardDtos {

    private BoardDtos() {
    }

    public record BoardResponse(
            Long id,
            String boardCode,
            String boardName,
            String authRead,
            String authWrite,
            boolean fileAllowed,
            boolean commentAllowed,
            boolean likeAllowed,
            int pageSize,
            /** 화면에서 "글쓰기" 버튼을 띄울지 판단하는 값 — 서버가 계산해 준다 */
            boolean canWrite,
            boolean guestWriteAllowed,
            long postCount
    ) {
    }

    /**
     * 글 작성. 비회원 글이면 {@code guestName}/{@code guestPwd} 가 필요하다.
     * 로그인 상태면 둘 다 무시된다.
     */
    public record PostCreateRequest(
            @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200) String title,
            @NotBlank(message = "내용을 입력해 주세요.") String content,
            @Size(max = 50) String guestName,
            @Size(min = 4, max = 30, message = "비밀번호는 4자 이상이어야 합니다.") String guestPwd
    ) {
    }

    /** 수정. 비회원 글이면 작성 시 정한 비밀번호를 함께 보내야 한다. */
    public record PostUpdateRequest(
            @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200) String title,
            @NotBlank(message = "내용을 입력해 주세요.") String content,
            String guestPwd
    ) {
    }

    public record PostSummary(
            Long id,
            String title,
            String writer,
            boolean guestPost,
            int viewCnt,
            int likeCnt,
            int commentCnt,
            boolean answered,
            int fileCount,
            LocalDateTime createdAt
    ) {
    }

    public record PostDetail(
            Long id,
            Long boardId,
            String boardCode,
            String boardName,
            String title,
            String content,
            String writer,
            boolean guestPost,
            int viewCnt,
            int likeCnt,
            int commentCnt,
            boolean answered,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            /** 현재 사용자가 이 글을 고칠 수 있는지 (비회원 글은 비밀번호 확인이 별도로 필요) */
            boolean editable,
            boolean likedByMe,
            boolean commentAllowed,
            boolean likeAllowed,
            List<FileResponse> files,
            List<CommentResponse> comments
    ) {
    }

    public record FileResponse(
            Long id,
            String origName,
            String contentType,
            String mediaType,
            long fileSize,
            int downloadCnt,
            String downloadUrl
    ) {
    }

    public record CommentCreateRequest(
            @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 2000) String content,
            Long parentId
    ) {
    }

    public record CommentUpdateRequest(
            @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 2000) String content
    ) {
    }

    public record CommentResponse(
            Long id,
            Long parentId,
            String writer,
            String content,
            boolean deleted,
            boolean editable,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record LikeResponse(
            Long postId,
            boolean liked,
            int likeCnt
    ) {
    }

    /** 비회원 글 수정 진입 전 비밀번호 확인 */
    public record GuestPasswordRequest(
            @NotBlank(message = "비밀번호를 입력해 주세요.") String guestPwd
    ) {
    }
}
