package triB.triB.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import triB.triB.community.dto.request.CommentCreateRequest;
import triB.triB.community.dto.response.CommentResponse;
import triB.triB.community.service.CommentService;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;

import java.util.List;

@Tag(name = "Community - Comment", description = "커뮤니티 댓글 API")
@RestController
@RequestMapping("/api/v1/community/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다. parentCommentId를 지정하면 대댓글이 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CommentCreateRequest request) {

        Long userId = userPrincipal.getUserId();
        CommentResponse response = commentService.createComment(postId, userId, request);

        return ApiResponse.created("댓글이 작성되었습니다.", response);
    }

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다. (대댓글 포함)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long currentUserId = userPrincipal != null ? userPrincipal.getUserId() : null;
        List<CommentResponse> response = commentService.getCommentsByPostId(postId, currentUserId);
        return ApiResponse.ok("댓글 목록 조회 성공", response);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 대댓글이 있는 경우 모두 삭제됩니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getUserId();
        commentService.deleteComment(commentId, userId);

        return ApiResponse.ok("댓글이 삭제되었습니다.", null);
    }

    @Operation(summary = "댓글 차단", description = "댓글을 차단합니다. 모든 사용자에게 보이지 않습니다.")
    @PostMapping("/{commentId}/block")
    public ResponseEntity<ApiResponse<Void>> blockComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getUserId();
        commentService.blockComment(commentId, userId);

        return ApiResponse.ok("댓글을 차단했습니다.", null);
    }

    @Operation(summary = "댓글 차단 해제", description = "차단된 댓글의 차단을 해제합니다.")
    @DeleteMapping("/{commentId}/block")
    public ResponseEntity<ApiResponse<Void>> unblockComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getUserId();
        commentService.unblockComment(commentId, userId);

        return ApiResponse.ok("댓글 차단을 해제했습니다.", null);
    }
}
