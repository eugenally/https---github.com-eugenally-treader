package com.edu.bootstring.board;

import com.edu.bootstring.board.dto.BoardDtos;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 CRUD.
 *
 * <p>{@code POST.COMMENT_CNT} 는 파생값이라 댓글이 바뀔 때마다 같은 트랜잭션에서 다시 센다.
 * 증감으로 처리하면 어딘가에서 한 번 빠뜨렸을 때 영영 어긋난다.
 */
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final PostCommentRepository commentRepository;
    private final PostService postService;
    private final BoardAccessPolicy accessPolicy;

    @Transactional
    public BoardDtos.PostDetail create(Long postId, BoardDtos.CommentCreateRequest req,
                                       MemberPrincipal principal) {
        Post post = postService.getPost(postId);
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        accessPolicy.checkComment(board, principal);

        if (req.parentId() != null) {
            PostComment parent = get(req.parentId());
            if (!parent.getPostId().equals(postId)) {
                throw new BusinessException("다른 글의 댓글에는 답글을 달 수 없습니다.",
                        ErrorCode.INVALID_INPUT_VALUE);
            }
            // 한 단계까지만 허용한다. 무제한 깊이는 화면도 쿼리도 감당하기 어렵다.
            if (parent.getParentId() != null) {
                throw new BusinessException("답글에는 다시 답글을 달 수 없습니다.",
                        ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        commentRepository.save(PostComment.builder()
                .postId(postId)
                .memberId(principal.memberId())
                .parentId(req.parentId())
                .content(req.content())
                .build());

        refreshCount(post);
        return postService.toDetail(post, board, principal);
    }

    @Transactional
    public BoardDtos.PostDetail update(Long commentId, BoardDtos.CommentUpdateRequest req,
                                       MemberPrincipal principal) {
        PostComment comment = get(commentId);
        requireModifiable(comment, principal);

        comment.update(req.content());

        Post post = postService.getPost(comment.getPostId());
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        return postService.toDetail(post, board, principal);
    }

    @Transactional
    public BoardDtos.PostDetail delete(Long commentId, MemberPrincipal principal) {
        PostComment comment = get(commentId);
        requireModifiable(comment, principal);

        // 대댓글이 달려 있으면 자리를 남겨야 트리가 끊기지 않는다. 어차피 소프트 삭제다.
        comment.softDelete();

        Post post = postService.getPost(comment.getPostId());
        refreshCount(post);

        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        return postService.toDetail(post, board, principal);
    }

    private void requireModifiable(PostComment comment, MemberPrincipal principal) {
        if (comment.isDeleted()) {
            throw new BusinessException("이미 삭제된 댓글입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!accessPolicy.canModifyComment(comment, principal)) {
            throw new BusinessException("이 댓글을 수정·삭제할 권한이 없습니다.", ErrorCode.ACCESS_DENIED);
        }
    }

    private void refreshCount(Post post) {
        post.applyCommentCount(commentRepository.countByPostIdAndDeletedFalse(post.getId()));
        postRepository.save(post);
    }

    private PostComment get(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다. id=" + commentId));
    }
}
