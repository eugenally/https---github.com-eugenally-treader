package com.edu.bootstring.board;

import com.edu.bootstring.board.dto.BoardDtos;
import com.edu.bootstring.global.common.PageResponse;
import com.edu.bootstring.global.security.MemberPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private static final String VIEW_COOKIE = "treader_viewed";

    private final PostService postService;
    private final PostCommentService commentService;
    private final PostLikeService likeService;
    private final PostFileService fileService;

    // ------------------------------------------------------------------
    // 게시판 · 목록
    // ------------------------------------------------------------------

    @GetMapping
    public List<BoardDtos.BoardResponse> boards() {
        return postService.findBoards(MemberPrincipal.current());
    }

    @GetMapping("/{boardCode}/posts")
    public PageResponse<BoardDtos.PostSummary> posts(
            @PathVariable String boardCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return postService.findPosts(boardCode, page, type, keyword, MemberPrincipal.current());
    }

    // ------------------------------------------------------------------
    // 글 상세 — 조회수는 쿠키로 중복을 막는다
    // ------------------------------------------------------------------

    /**
     * 조회수 증가 여부를 <b>서버가</b> 판단한다.
     * 클라이언트가 "이번엔 세 주세요"를 보내게 하면 새로고침으로 얼마든지 부풀릴 수 있다.
     *
     * <p>쿠키에 읽은 글 번호를 누적하고, 자정에 만료시켜 하루 한 번만 센다.
     */
    @GetMapping("/posts/{postId}")
    public BoardDtos.PostDetail post(@PathVariable Long postId,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String viewed = readCookie(request);
        boolean alreadyViewed = containsPostId(viewed, postId);

        if (!alreadyViewed) {
            writeCookie(response, viewed == null ? String.valueOf(postId) : viewed + "_" + postId);
        }
        return postService.findPost(postId, !alreadyViewed, MemberPrincipal.current());
    }

    // ------------------------------------------------------------------
    // 글 작성 · 수정 · 삭제
    // ------------------------------------------------------------------

    @PostMapping("/{boardCode}/posts")
    public BoardDtos.PostDetail create(@PathVariable String boardCode,
                                       @Valid @RequestBody BoardDtos.PostCreateRequest req,
                                       HttpServletRequest request) {
        return postService.create(boardCode, req, MemberPrincipal.current(), clientIp(request));
    }

    @PutMapping("/posts/{postId}")
    public BoardDtos.PostDetail update(@PathVariable Long postId,
                                       @Valid @RequestBody BoardDtos.PostUpdateRequest req) {
        return postService.update(postId, req, MemberPrincipal.current());
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId,
                                       @RequestParam(required = false) String guestPwd) {
        postService.delete(postId, guestPwd, MemberPrincipal.current());
        return ResponseEntity.noContent().build();
    }

    /** 비회원 글 수정 화면 진입 전 확인 */
    @PostMapping("/posts/{postId}/verify-password")
    public ResponseEntity<Map<String, String>> verifyPassword(
            @PathVariable Long postId,
            @Valid @RequestBody BoardDtos.GuestPasswordRequest req) {
        postService.verifyGuestPassword(postId, req.guestPwd());
        return ResponseEntity.ok(Map.of("message", "확인되었습니다."));
    }

    /** Q&A 답변 완료 표시 */
    @PostMapping("/posts/{postId}/answered")
    public BoardDtos.PostDetail markAnswered(@PathVariable Long postId,
                                             @RequestParam(defaultValue = "true") boolean value) {
        return postService.markAnswered(postId, value, MemberPrincipal.current());
    }

    // ------------------------------------------------------------------
    // 댓글
    // ------------------------------------------------------------------

    @PostMapping("/posts/{postId}/comments")
    public BoardDtos.PostDetail addComment(@PathVariable Long postId,
                                           @Valid @RequestBody BoardDtos.CommentCreateRequest req) {
        return commentService.create(postId, req, MemberPrincipal.current());
    }

    @PutMapping("/comments/{commentId}")
    public BoardDtos.PostDetail updateComment(@PathVariable Long commentId,
                                              @Valid @RequestBody BoardDtos.CommentUpdateRequest req) {
        return commentService.update(commentId, req, MemberPrincipal.current());
    }

    @DeleteMapping("/comments/{commentId}")
    public BoardDtos.PostDetail deleteComment(@PathVariable Long commentId) {
        return commentService.delete(commentId, MemberPrincipal.current());
    }

    // ------------------------------------------------------------------
    // 좋아요
    // ------------------------------------------------------------------

    @PostMapping("/posts/{postId}/like")
    public BoardDtos.LikeResponse toggleLike(@PathVariable Long postId) {
        return likeService.toggle(postId, MemberPrincipal.current());
    }

    // ------------------------------------------------------------------
    // 파일
    // ------------------------------------------------------------------

    @PostMapping(value = "/posts/{postId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<BoardDtos.FileResponse> uploadFiles(@PathVariable Long postId,
                                                    @RequestPart("files") List<MultipartFile> files) {
        return fileService.upload(postId, files, MemberPrincipal.current()).stream()
                .map(f -> new BoardDtos.FileResponse(
                        f.getId(), f.getOrigName(), f.getContentType(), f.getMediaType(),
                        f.getFileSize(), f.getDownloadCnt(), "/api/boards/files/" + f.getId()))
                .toList();
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long fileId) {
        PostFile file = fileService.loadForDownload(fileId, MemberPrincipal.current());
        var resource = fileService.resolveResource(file);

        // 이미지·영상은 화면에서 바로 보여줘야 하므로 inline, 나머지는 저장 유도
        boolean inline = !"OTHER".equals(file.getMediaType());
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(file.getOrigName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(file.getContentType() != null
                        ? MediaType.parseMediaType(file.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        fileService.delete(fileId, MemberPrincipal.current());
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // 조회수 쿠키
    // ------------------------------------------------------------------

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> VIEW_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean containsPostId(String cookieValue, Long postId) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return false;
        }
        // "_12_" 형태로 감싸 비교해야 1 과 12 가 섞이지 않는다
        return ("_" + cookieValue + "_").contains("_" + postId + "_");
    }

    private void writeCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(VIEW_COOKIE, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // 자정에 만료 — 하루가 지나면 같은 글도 다시 한 번 센다
        long secondsToMidnight = Duration.between(
                LocalDateTime.now(),
                LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
        cookie.setMaxAge((int) Math.max(secondsToMidnight, 1));
        response.addCookie(cookie);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
