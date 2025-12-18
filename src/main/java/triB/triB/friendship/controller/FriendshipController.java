package triB.triB.friendship.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import triB.triB.friendship.dto.FriendRequest;
import triB.triB.friendship.dto.NewUserResponse;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.friendship.service.FriendshipService;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listMyFriends(@AuthenticationPrincipal UserPrincipal UserPrincipal){
        Long userId = UserPrincipal.getUserId();
        List<UserResponse> responses = friendshipService.getMyFriends(userId);
        return ApiResponse.ok("유저의 친구 목록을 조회했습니다.", responses);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> myProfile(@AuthenticationPrincipal UserPrincipal UserPrincipal){
        Long userId = UserPrincipal.getUserId();
        UserResponse response = friendshipService.getMyProfile(userId);
        return ApiResponse.ok("유저의 프로필을 조회했습니다.", response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchFriends(
            @AuthenticationPrincipal UserPrincipal UserPrincipal,
            @RequestParam(name = "nickname") String nickname
    ){
        Long userId = UserPrincipal.getUserId();
        List<UserResponse> responses = friendshipService.searchMyFriends(userId, nickname);
        return ApiResponse.ok("친구를 검색했습니다.", responses);
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<NewUserResponse>> searchNewFriend(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "username") String username
    ){
        Long userId = userPrincipal.getUserId();
        NewUserResponse response = friendshipService.searchNewFriend(userId, username);
        return ApiResponse.ok("해당 아이디의 유저를 조회했습니다.", response);
    }

    @PostMapping("{userId}/request")
    public ResponseEntity<ApiResponse<Void>> requestFriendship(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "userId") Long userId2
    ) {
        Long userId1 =  userPrincipal.getUserId();
        friendshipService.requestFriendshipToUser(userId1, userId2);
        return ApiResponse.created("친구 요청을 보냈습니다.", null);
    }

    @GetMapping("/me/request")
    public ResponseEntity<ApiResponse<List<FriendRequest>>> listMyRequests(@AuthenticationPrincipal UserPrincipal UserPrincipal){
        Long userId = UserPrincipal.getUserId();
        List<FriendRequest> response = friendshipService.getMyRequests(userId);
        return ApiResponse.ok("친구초대 목록을 조회했습니다.", response);
    }

    @PostMapping("{friendshipId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFriendship(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "friendshipId") Long friendshipId
    ) throws FirebaseMessagingException {
        Long userId = userPrincipal.getUserId();
        friendshipService.acceptMyFriendship(userId, friendshipId);
        return ApiResponse.ok("친구 요청을 수락했습니다.", null);
    }

    @PostMapping("{friendshipId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriendship(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "friendshipId") Long friendshipId
    ){
        Long userId = userPrincipal.getUserId();
        friendshipService.rejectMyFriendship(userId, friendshipId);
        return ApiResponse.ok("친구 요청을 거절했습니다.", null);
    }
}
