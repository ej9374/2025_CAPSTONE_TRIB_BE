package triB.triB.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.dto.PostSortType;
import triB.triB.community.dto.request.FreeBoardPostCreateRequest;
import triB.triB.community.dto.request.FreeBoardPostFilterRequest;
import triB.triB.community.dto.request.TripSharePostCreateRequest;
import triB.triB.community.dto.request.TripSharePostFilterRequest;
import triB.triB.community.dto.response.HotPostResponse;
import triB.triB.community.dto.response.PostDetailsResponse;
import triB.triB.community.dto.response.PostLikeResponse;
import triB.triB.community.dto.response.PostSummaryResponse;
import triB.triB.community.dto.response.TripSharePreviewResponse;
import triB.triB.community.entity.Hashtag;
import triB.triB.community.entity.Post;
import triB.triB.community.entity.PostType;
import triB.triB.community.entity.TagType;
import triB.triB.community.repository.HashtagRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.community.service.HotPostScheduler;
import triB.triB.community.service.PostLikeService;
import triB.triB.community.service.PostService;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;
import triB.triB.schedule.dto.TripScheduleResponse;
import triB.triB.schedule.service.ScheduleService;

import java.util.stream.Collectors;

import java.util.List;

@Tag(name = "Community - Post", description = "커뮤니티 게시글 API")
@RestController
@RequestMapping("/api/v1/community/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final HashtagRepository hashtagRepository;
    private final HotPostScheduler hotPostScheduler;
    private final PostRepository postRepository;
    private final ScheduleService scheduleService;

    @Operation(summary = "일정 공유 게시글 미리보기",
               description = "일정 공유 게시글 작성 전 AI가 생성한 해시태그와 여행 일정을 미리 조회합니다.")
    @PostMapping("/trip-share/preview")
    public ResponseEntity<ApiResponse<TripSharePreviewResponse>> getTripSharePreview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("tripId") Long tripId) {

        Long userId = userPrincipal.getUserId();
        TripSharePreviewResponse response = postService.getTripSharePreview(userId, tripId);

        return ApiResponse.ok("일정 공유 미리보기 정보를 조회했습니다.", response);
    }

    @Operation(summary = "일정 공유 게시글 작성",
               description = "TRIP_SHARE 타입 게시글을 작성합니다. tripId, content, 선택한 해시태그, 이미지를 함께 업로드합니다.")
    @PostMapping(value = "/trip-share", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDetailsResponse>> createTripSharePost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestPart("request") TripSharePostCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        Long userId = userPrincipal.getUserId();
        PostDetailsResponse response = postService.createTripSharePost(userId, request, images);

        return ApiResponse.created("일정 공유 게시글이 작성되었습니다.", response);
    }

    @Operation(summary = "자유게시판 게시글 작성",
               description = "FREE_BOARD 타입 게시글을 작성합니다. Predefined 해시태그를 선택할 수 있습니다.")
    @PostMapping(value = "/free-board", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDetailsResponse>> createFreeBoardPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("title") @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.") String title,
            @RequestParam("content") @NotBlank(message = "내용은 필수입니다.") @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.") String content,
            @RequestParam("hashtags") @NotEmpty(message = "최소 1개 이상의 해시태그를 선택해주세요.") @Size(max = 7, message = "해시태그는 최대 7개까지 선택 가능합니다.") List<String> hashtags,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        FreeBoardPostCreateRequest request = new FreeBoardPostCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setHashtags(hashtags);

        Long userId = userPrincipal.getUserId();
        PostDetailsResponse response = postService.createFreeBoardPost(userId, request, images);

        return ApiResponse.created("자유게시판 게시글이 작성되었습니다.", response);
    }

    @Operation(summary = "게시글 상세 조회",
               description = "게시글의 상세 정보를 조회합니다. (작성자, 이미지, 해시태그, 좋아요/댓글 수 포함)")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailsResponse>> getPostDetails(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long currentUserId = userPrincipal != null ? userPrincipal.getUserId() : null;
        PostDetailsResponse response = postService.getPostDetails(postId, currentUserId);

        return ApiResponse.ok("게시글 상세 조회 성공", response);
    }

    @Operation(summary = "일정 공유 게시판 목록 조회",
               description = "나라, 정렬 조건, 검색어를 동시에 적용하여 TRIP_SHARE 게시글 목록을 조회합니다.")
    @GetMapping("/trip-share")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> getTripSharePosts(
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "LATEST") PostSortType sortType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        TripSharePostFilterRequest filter = TripSharePostFilterRequest.builder()
                .country(country)
                .sortType(sortType)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        List<PostSummaryResponse> response = postService.getTripSharePosts(filter);
        return ApiResponse.ok("일정 공유 게시판 목록 조회 성공", response);
    }

    @Operation(summary = "자유게시판 목록 조회",
               description = "제목 검색, 정렬 조건, 해시태그 필터를 동시에 적용하여 FREE_BOARD 게시글 목록을 조회합니다.")
    @GetMapping("/free-board")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> getFreeBoardPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") PostSortType sortType,
            @RequestParam(required = false) List<String> hashtags,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        FreeBoardPostFilterRequest filter = FreeBoardPostFilterRequest.builder()
                .keyword(keyword)
                .sortType(sortType)
                .hashtags(hashtags)
                .page(page)
                .size(size)
                .build();

        List<PostSummaryResponse> response = postService.getFreeBoardPosts(filter);
        return ApiResponse.ok("자유게시판 목록 조회 성공", response);
    }

    @Operation(summary = "핫 게시글 조회",
               description = "최근 1시간 내 좋아요+댓글 수가 가장 많은 자유게시판 게시글 1개를 조회합니다. 1시간마다 자동 갱신됩니다.")
    @GetMapping("/free-board/hot")
    public ResponseEntity<ApiResponse<HotPostResponse>> getHotPost() {
        HotPostResponse hotPost = hotPostScheduler.getHotPost();

        if (hotPost == null) {
            return ApiResponse.ok("현재 핫 게시글이 없습니다.", null);
        }

        return ApiResponse.ok("핫 게시글 조회 성공", hotPost);
    }

    @Operation(summary = "Predefined 해시태그 목록 조회",
               description = "자유게시판에서 사용할 수 있는 Predefined 해시태그 목록을 조회합니다.")
    @GetMapping("/hashtags/predefined")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getPredefinedHashtags() {
        List<Hashtag> hashtags = hashtagRepository.findByTagType(TagType.PREDEFINED);
        List<HashtagResponse> response = hashtags.stream()
                .map(HashtagResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.ok("Predefined 해시태그 목록 조회 성공", response);
    }

    @Operation(summary = "게시글 좋아요 토글",
               description = "게시글 좋아요를 추가하거나 취소합니다. 이미 좋아요를 누른 경우 취소되고, 안 누른 경우 추가됩니다.")
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> togglePostLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getUserId();
        postLikeService.toggleLike(postId, userId);

        // 토글 후 현재 상태 조회
        boolean isLiked = postLikeService.isLikedByUser(postId, userId);
        PostDetailsResponse postDetails = postService.getPostDetails(postId, userId);

        PostLikeResponse response = PostLikeResponse.of(isLiked, postDetails.getLikesCount());

        String message = isLiked ? "게시글에 좋아요를 추가했습니다." : "게시글 좋아요를 취소했습니다.";
        return ApiResponse.ok(message, response);
    }

    @Operation(summary = "일정 공유 게시글의 일정 조회",
               description = "TRIP_SHARE 타입 게시글의 특정 날짜 일정을 조회합니다. 누구나 조회 가능합니다.")
    @GetMapping("/{postId}/schedules")
    public ResponseEntity<ApiResponse<TripScheduleResponse>> getSchedulesFromPost(
            @PathVariable Long postId,
            @RequestParam(required = false, defaultValue = "1") Integer dayNumber) {

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // TRIP_SHARE 타입인지 검증
        if (post.getPostType() != PostType.TRIP_SHARE) {
            throw new IllegalArgumentException("일정 공유 게시글만 일정을 조회할 수 있습니다.");
        }

        // tripId 확인
        if (post.getTripId() == null) {
            throw new IllegalArgumentException("게시글에 연결된 여행 정보가 없습니다.");
        }

        // 일정 조회 (권한 검증 없이 공개 조회)
        TripScheduleResponse response = scheduleService.getTripSchedulesPublic(
                post.getTripId(),
                dayNumber
        );

        return ApiResponse.ok("일정을 조회했습니다.", response);
    }

    @Operation(
            summary = "게시글 삭제",
            description = """
                    게시글을 삭제합니다.
                    - 작성자 본인만 삭제할 수 있습니다.
                    - 게시글과 함께 연관된 이미지, 댓글, 좋아요, 해시태그도 모두 삭제됩니다.
                    - S3에 업로드된 이미지 파일도 함께 삭제됩니다.
                    """
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal.getUserId();
        postService.deletePost(postId, userId);

        return ApiResponse.ok("게시글이 삭제되었습니다.", null);
    }
}
