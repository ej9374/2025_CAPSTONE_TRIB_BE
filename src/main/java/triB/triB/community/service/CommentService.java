package triB.triB.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.UserRepository;
import triB.triB.community.dto.request.CommentCreateRequest;
import triB.triB.community.dto.response.CommentResponse;
import triB.triB.community.entity.Comment;
import triB.triB.community.entity.Post;
import triB.triB.community.exception.CommentNotFoundException;
import triB.triB.community.exception.PostNotFoundException;
import triB.triB.community.exception.UnauthorizedPostAccessException;
import triB.triB.community.entity.CommentBlock;
import triB.triB.community.entity.CommentBlockId;
import triB.triB.community.repository.CommentBlockRepository;
import triB.triB.community.repository.CommentRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentBlockRepository commentBlockRepository;
    private final triB.triB.global.utils.CheckBadWordsUtil checkBadWordsUtil;
    private final UserBlockService userBlockService;

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
        // 1. Post 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_IN_TRIP)); // User 에러코드 필요 시 수정

        // 3. 욕설 검증
        checkBadWordsUtil.validateNoBadWords(request.getContent());

        // 4. 대댓글인 경우 부모 댓글 확인
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(CommentNotFoundException::new);

            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (!parentComment.getPostId().equals(postId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_POST_ACCESS);
            }
        }

        // 5. Comment 엔티티 생성
        Comment comment = Comment.builder()
                .postId(postId)
                .post(post)
                .userId(userId)
                .user(user)
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 6. Post의 댓글 수 증가
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return CommentResponse.from(savedComment, user, new ArrayList<>());
    }

    public List<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId) {
        // 현재 유저가 차단한 유저 및 댓글 목록 조회
        List<Long> blockedUserIds = new ArrayList<>();
        List<Long> blockedCommentIds = new ArrayList<>();
        if (currentUserId != null) {
            blockedUserIds = userBlockService.getBlockedUserIds(currentUserId);
            blockedCommentIds = getBlockedCommentIds(currentUserId);
        }

        // 최상위 댓글만 조회
        final List<Long> finalBlockedUserIds = blockedUserIds;
        final List<Long> finalBlockedCommentIds = blockedCommentIds;
        List<Comment> topLevelComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .filter(comment -> comment.getParentCommentId() == null)
                .filter(comment -> !finalBlockedCommentIds.contains(comment.getCommentId()))  // 차단된 댓글 제외
                .filter(comment -> !finalBlockedUserIds.contains(comment.getUserId()))  // 차단된 유저의 댓글 제외
                .collect(Collectors.toList());

        return topLevelComments.stream()
                .map(comment -> mapToResponseWithReplies(comment, finalBlockedUserIds, finalBlockedCommentIds))
                .collect(Collectors.toList());
    }

    private CommentResponse mapToResponseWithReplies(Comment comment, List<Long> blockedUserIds, List<Long> blockedCommentIds) {
        User author = userRepository.findById(comment.getUserId()).orElse(null);

        // 대댓글 조회 (차단 필터링 적용)
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getCommentId())
                .stream()
                .filter(reply -> !blockedCommentIds.contains(reply.getCommentId()))  // 차단된 댓글 제외
                .filter(reply -> !blockedUserIds.contains(reply.getUserId()))  // 차단된 유저의 댓글 제외
                .collect(Collectors.toList());

        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> {
                    User replyAuthor = userRepository.findById(reply.getUserId()).orElse(null);
                    return CommentResponse.from(reply, replyAuthor, new ArrayList<>());
                })
                .collect(Collectors.toList());

        return CommentResponse.from(comment, author, replyResponses);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedPostAccessException();
        }

        // 대댓글이 있는 경우 모두 삭제
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        commentRepository.deleteAll(replies);

        // 댓글 삭제
        commentRepository.delete(comment);

        // Post의 댓글 수 감소
        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(PostNotFoundException::new);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - (1 + replies.size())));
        postRepository.save(post);
    }

    /**
     * 댓글 차단 (개인적 차단)
     */
    @Transactional
    public void blockComment(Long commentId, Long currentUserId) {
        // 댓글 존재 확인
        commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 이미 차단된 경우 무시
        if (commentBlockRepository.existsByIdBlockerUserIdAndIdBlockedCommentId(currentUserId, commentId)) {
            return;
        }

        // CommentBlock 관계 생성
        CommentBlockId id = new CommentBlockId(currentUserId, commentId);
        CommentBlock commentBlock = CommentBlock.builder()
                .id(id)
                .build();
        commentBlockRepository.save(commentBlock);
    }

    /**
     * 댓글 차단 해제
     */
    @Transactional
    public void unblockComment(Long commentId, Long currentUserId) {
        // CommentBlock 관계 삭제
        CommentBlockId id = new CommentBlockId(currentUserId, commentId);
        commentBlockRepository.deleteById(id);
    }

    /**
     * 현재 유저가 차단한 댓글 ID 목록 조회
     */
    public List<Long> getBlockedCommentIds(Long currentUserId) {
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        return commentBlockRepository.findBlockedCommentIdsByBlockerUserId(currentUserId);
    }
}
