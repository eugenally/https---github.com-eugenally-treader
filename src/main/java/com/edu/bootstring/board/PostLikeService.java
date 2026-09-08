package com.edu.bootstring.board;

import com.edu.bootstring.board.dto.BoardDtos;
import com.edu.bootstring.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 토글.
 *
 * <p>{@code POST_LIKE} 가 진실의 원천이고 {@code POST.LIKE_CNT} 는 목록 조회를 위한 파생값이다.
 * 두 값이 어긋나지 않도록 같은 트랜잭션에서 다시 세어 넣는다.
 */
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final PostLikeRepository likeRepository;
    private final PostService postService;
    private final BoardAccessPolicy accessPolicy;

    @Transactional
    public BoardDtos.LikeResponse toggle(Long postId, MemberPrincipal principal) {
        Post post = postService.getPost(postId);
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        accessPolicy.checkLike(board, principal);

        Long memberId = principal.memberId();
        boolean liked;

        if (likeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            likeRepository.deleteByPostIdAndMemberId(postId, memberId);
            liked = false;
        } else {
            likeRepository.save(new PostLike(postId, memberId));
            liked = true;
        }
        likeRepository.flush();     // 카운트를 다시 세기 전에 DB 에 반영해 둔다

        int count = likeRepository.countByPostId(postId);
        post.applyLikeCount(count);
        postRepository.save(post);

        return new BoardDtos.LikeResponse(postId, liked, count);
    }
}
