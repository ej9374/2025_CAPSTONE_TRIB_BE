package triB.triB.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import triB.triB.community.entity.PostType;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;
import triB.triB.user.dto.PostResponse;
import triB.triB.user.service.UserCommunityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/community")
@RequiredArgsConstructor
public class UserCommunityController {

    private final UserCommunityService userCommunityService;

    @GetMapping("/trip/me")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getMyTripPost(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<PostResponse> response = userCommunityService.getMyPosts(userId, PostType.TRIP_SHARE);
        return ApiResponse.ok("유저가 작성한 게시글을 조회했습니다.", response);
    }

    @GetMapping("/post/me")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getMyPost(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<PostResponse> response = userCommunityService.getMyPosts(userId, PostType.FREE_BOARD);
        return ApiResponse.ok("유저가 작성한 게시글을 조회했습니다.", response);
    }

    @GetMapping("/trip/like/me")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getLikeTripPost(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<PostResponse> response = userCommunityService.getLikePosts(userId, PostType.TRIP_SHARE);
        return ApiResponse.ok("유저가 좋아요한 게시글을 조회했습니다.", response);
    }

    @GetMapping("/post/like/me")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getLikePost(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        List<PostResponse> response = userCommunityService.getLikePosts(userId, PostType.FREE_BOARD);
        return ApiResponse.ok("유저가 좋아요한 게시글을 조회했습니다.", response);
    }
}