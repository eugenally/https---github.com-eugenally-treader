package com.edu.bootstring.board;

import com.edu.bootstring.board.dto.BoardDtos;
import com.edu.bootstring.global.common.PageResponse;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.global.security.MemberPrincipal;
import com.edu.bootstring.member.Member;
import com.edu.bootstring.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 CRUD. 게시판 세 종류를 {@code boardCode} 하나로 갈라 한 벌로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostFileRepository fileRepository;
    private final PostLikeRepository likeRepository;
    private final MemberRepository memberRepository;
    private final BoardAccessPolicy accessPolicy;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------
    // 게시판
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BoardDtos.BoardResponse> findBoards(MemberPrincipal principal) {
        return boardRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(b -> accessPolicy.canRead(b, principal))
                .map(b -> new BoardDtos.BoardResponse(
                        b.getId(),
                        b.getBoardCode(),
                        b.getBoardName(),
                        b.getAuthRead(),
                        b.getAuthWrite(),
                        b.isFileAllowed(),
                        b.isCommentAllowed(),
                        b.isLikeAllowed(),
                        b.getPageSize(),
                        accessPolicy.canWrite(b, principal),
                        b.allowsGuestWrite(),
                        postRepository.countByBoardIdAndDeletedFalse(b.getId())))
                .toList();
    }

    // ------------------------------------------------------------------
    // 목록
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<BoardDtos.PostSummary> findPosts(String boardCode, int page, String type,
                                                         String keyword, MemberPrincipal principal) {
        Board board = getBoard(boardCode);
        accessPolicy.checkRead(board, principal);

        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0),                       // 화면은 1-based, Spring 은 0-based
                board.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));

        Page<Post> result = searchPage(board.getId(), type, keyword, pageable);

        Map<Long, String> writers = writerNames(result.getContent());
        Map<Long, Integer> fileCounts = fileCounts(result.getContent());

        return PageResponse.from(result, p -> new BoardDtos.PostSummary(
                p.getId(),
                p.getTitle(),
                writers.get(p.getId()),
                p.isGuestPost(),
                p.getViewCnt(),
                p.getLikeCnt(),
                p.getCommentCnt(),
                p.isAnswered(),
                fileCounts.getOrDefault(p.getId(), 0),
                p.getCreatedAt()));
    }

    /** 검색어가 없으면 전체 목록, 있으면 조건별 메서드로 보낸다 */
    private Page<Post> searchPage(Long boardId, String type, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return postRepository.findByBoardIdAndDeletedFalse(boardId, pageable);
        }
        String kw = keyword.trim();
        return switch (type == null ? "ALL" : type.toUpperCase()) {
            case "TITLE" -> postRepository.searchByTitle(boardId, kw, pageable);
            case "CONTENT" -> postRepository.searchByContent(boardId, kw, pageable);
            case "WRITER" -> postRepository.searchByWriter(boardId, kw, pageable);
            default -> postRepository.searchByTitleOrContent(boardId, kw, pageable);
        };
    }

    // ------------------------------------------------------------------
    // 상세 — 조회수 증가는 컨트롤러가 쿠키를 보고 결정한다
    // ------------------------------------------------------------------

    @Transactional
    public BoardDtos.PostDetail findPost(Long postId, boolean countView, MemberPrincipal principal) {
        Post post = getPost(postId);
        Board board = boardRepository.findById(post.getBoardId())
                .orElseThrow(() -> new NotFoundException("게시판을 찾을 수 없습니다."));
        accessPolicy.checkRead(board, principal);

        if (countView) {
            post.increaseViewCount();
        }

        return toDetail(post, board, principal);
    }

    // ------------------------------------------------------------------
    // 작성 · 수정 · 삭제
    // ------------------------------------------------------------------

    @Transactional
    public BoardDtos.PostDetail create(String boardCode, BoardDtos.PostCreateRequest req,
                                       MemberPrincipal principal, String clientIp) {
        Board board = getBoard(boardCode);
        accessPolicy.checkWrite(board, principal);

        Post.PostBuilder builder = Post.builder()
                .boardId(board.getId())
                .title(req.title())
                .content(req.content())
                .writerIp(clientIp);

        if (principal != null) {
            builder.memberId(principal.memberId());
        } else {
            // 비회원 글은 이름과 비밀번호가 있어야 나중에 본인 확인이 된다
            if (req.guestName() == null || req.guestName().isBlank()) {
                throw new BusinessException("이름을 입력해 주세요.", ErrorCode.INVALID_INPUT_VALUE);
            }
            if (req.guestPwd() == null || req.guestPwd().length() < 4) {
                throw new BusinessException("비밀번호를 4자 이상 입력해 주세요.", ErrorCode.INVALID_INPUT_VALUE);
            }
            builder.guestName(req.guestName())
                    .guestPwd(passwordEncoder.encode(req.guestPwd()));
        }

        Post saved = postRepository.save(builder.build());
        return toDetail(saved, board, principal);
    }

    @Transactional
    public BoardDtos.PostDetail update(Long postId, BoardDtos.PostUpdateRequest req,
                                       MemberPrincipal principal) {
        Post post = getPost(postId);
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();

        requireModifiable(post, principal, req.guestPwd());
        post.update(req.title(), req.content());

        return toDetail(post, board, principal);
    }

    @Transactional
    public void delete(Long postId, String guestPwd, MemberPrincipal principal) {
        Post post = getPost(postId);
        requireModifiable(post, principal, guestPwd);
        post.softDelete();
    }

    /** 비회원 글 수정 화면에 들어가기 전 비밀번호를 먼저 확인한다 */
    @Transactional(readOnly = true)
    public void verifyGuestPassword(Long postId, String guestPwd) {
        Post post = getPost(postId);
        if (!post.isGuestPost()) {
            throw new BusinessException("비회원 글이 아닙니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        checkGuestPassword(post, guestPwd);
    }

    /** Q&A 답변 완료 표시 — 관리자·영업담당만 */
    @Transactional
    public BoardDtos.PostDetail markAnswered(Long postId, boolean answered, MemberPrincipal principal) {
        if (principal == null || "CUSTOMER".equals(principal.role())) {
            throw new BusinessException("답변 상태는 담당자만 바꿀 수 있습니다.", ErrorCode.ACCESS_DENIED);
        }
        Post post = getPost(postId);
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        post.markAnswered(answered);
        return toDetail(post, board, principal);
    }

    // ------------------------------------------------------------------
    // 내부
    // ------------------------------------------------------------------

    private void requireModifiable(Post post, MemberPrincipal principal, String guestPwd) {
        if (!accessPolicy.canModifyPost(post, principal)) {
            throw new BusinessException("이 글을 수정·삭제할 권한이 없습니다.", ErrorCode.ACCESS_DENIED);
        }
        // 비회원 글은 관리자가 아닌 이상 비밀번호를 확인한다
        if (post.isGuestPost() && !(principal != null && "ADMIN".equals(principal.role()))) {
            checkGuestPassword(post, guestPwd);
        }
    }

    private void checkGuestPassword(Post post, String guestPwd) {
        if (guestPwd == null || !passwordEncoder.matches(guestPwd, post.getGuestPwd())) {
            throw new BusinessException("비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
        }
    }

    Board getBoard(String boardCode) {
        return boardRepository.findByBoardCode(boardCode)
                .orElseThrow(() -> new NotFoundException("게시판을 찾을 수 없습니다. code=" + boardCode));
    }

    Post getPost(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다. id=" + postId));
    }

    BoardDtos.PostDetail toDetail(Post post, Board board, MemberPrincipal principal) {
        List<BoardDtos.FileResponse> files = fileRepository.findAllByPostIdOrderByIdAsc(post.getId())
                .stream()
                .map(f -> new BoardDtos.FileResponse(
                        f.getId(), f.getOrigName(), f.getContentType(), f.getMediaType(),
                        f.getFileSize(), f.getDownloadCnt(), "/api/boards/files/" + f.getId()))
                .toList();

        List<PostComment> comments = commentRepository.findAllByPostIdOrderByIdAsc(post.getId());
        Map<Long, String> commentWriters = memberNames(
                comments.stream().map(PostComment::getMemberId).distinct().toList());

        List<BoardDtos.CommentResponse> commentResponses = comments.stream()
                .map(c -> new BoardDtos.CommentResponse(
                        c.getId(),
                        c.getParentId(),
                        c.isDeleted() ? null : commentWriters.get(c.getMemberId()),
                        c.isDeleted() ? "삭제된 댓글입니다." : c.getContent(),
                        c.isDeleted(),
                        !c.isDeleted() && accessPolicy.canModifyComment(c, principal),
                        c.getCreatedAt(),
                        c.getUpdatedAt()))
                .toList();

        boolean likedByMe = principal != null
                && likeRepository.existsByPostIdAndMemberId(post.getId(), principal.memberId());

        return new BoardDtos.PostDetail(
                post.getId(),
                board.getId(),
                board.getBoardCode(),
                board.getBoardName(),
                post.getTitle(),
                post.getContent(),
                writerName(post),
                post.isGuestPost(),
                post.getViewCnt(),
                post.getLikeCnt(),
                post.getCommentCnt(),
                post.isAnswered(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                accessPolicy.canModifyPost(post, principal),
                likedByMe,
                board.isCommentAllowed(),
                board.isLikeAllowed(),
                files,
                commentResponses);
    }

    private String writerName(Post post) {
        if (post.isGuestPost()) {
            return post.getGuestName();
        }
        return memberRepository.findById(post.getMemberId())
                .map(Member::getName)
                .orElse("(탈퇴한 회원)");
    }

    private Map<Long, String> writerNames(List<Post> posts) {
        Map<Long, String> memberNames = memberNames(
                posts.stream().map(Post::getMemberId).filter(java.util.Objects::nonNull).distinct().toList());

        return posts.stream().collect(Collectors.toMap(
                Post::getId,
                p -> p.isGuestPost()
                        ? p.getGuestName()
                        : memberNames.getOrDefault(p.getMemberId(), "(탈퇴한 회원)")));
    }

    private Map<Long, String> memberNames(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));
    }

    private Map<Long, Integer> fileCounts(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return fileRepository.countGroupedByPostId(postIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));
    }
}
