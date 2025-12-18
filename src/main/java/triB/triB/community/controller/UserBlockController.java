package triB.triB.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import triB.triB.community.service.UserBlockService;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;

import java.util.List;

@Tag(name = "Community - User Block", description = "유저 차단 API")
@RestController
@RequestMapping("/api/v1/community/blocks")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @Operation(summary = "유저 차단", description = "특정 유저를 차단합니다. 차단된 유저의 게시글과 댓글이 보이지 않습니다.")
    @PostMapping("/users/{blockedUserId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable Long blockedUserId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long blockerUserId = userPrincipal.getUserId();
        userBlockService.blockUser(blockerUserId, blockedUserId);

        return ApiResponse.ok("유저를 차단했습니다.", null);
    }

    @Operation(summary = "유저 차단 해제", description = "차단한 유저의 차단을 해제합니다.")
    @DeleteMapping("/users/{blockedUserId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable Long blockedUserId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long blockerUserId = userPrincipal.getUserId();
        userBlockService.unblockUser(blockerUserId, blockedUserId);

        return ApiResponse.ok("유저 차단을 해제했습니다.", null);
    }

    @Operation(summary = "차단한 유저 목록", description = "내가 차단한 유저 목록을 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long blockerUserId = userPrincipal.getUserId();
        List<UserResponse> blockedUsers = userBlockService.getBlockedUsers(blockerUserId);

        return ApiResponse.ok("차단한 유저 목록을 조회했습니다.", blockedUsers);
    }
}
