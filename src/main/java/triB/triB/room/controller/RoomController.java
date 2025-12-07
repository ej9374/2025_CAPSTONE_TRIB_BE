package triB.triB.room.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.room.dto.*;
import triB.triB.room.service.RoomService;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<RoomsResponse>>>> getRooms(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        Map<String, List<RoomsResponse>> rooms = new HashMap<>();
        List<RoomsResponse> roomList = roomService.getRoomList(userId);
        rooms.put("rooms", roomList);
        return ApiResponse.ok("유저의 채팅방 목록을 조회했습니다.", rooms);
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<Map<String, List<RoomsResponse>>>> searchRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String content
    ) {
        Long userId = userPrincipal.getUserId();
        Map<String, List<RoomsResponse>> rooms = new HashMap<>();
        List<RoomsResponse> roomList = roomService.searchRoomList(userId, content);
        rooms.put("rooms", roomList);
        return ApiResponse.ok("유저의 채팅방 목록을 조회했습니다.", rooms);
    }

    @PostMapping("/users/summary")
    public ResponseEntity<ApiResponse<List<ChatUserResponse>>> getCreateUsers(@RequestBody UserRequest userRequest) {
        List<ChatUserResponse> users = roomService.selectFriends(userRequest.getUserIds());
        return ApiResponse.ok("유저 프로필을 불러왔습니다.", users);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody RoomRequest roomRequest
    ){
        Long userId = userPrincipal.getUserId();
        RoomResponse room = roomService.makeChatRoom(userId, roomRequest);
        return ApiResponse.created("채팅방을 생성했습니다.", room);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> deleteRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "roomId") Long roomId
    ){
        Long userId = userPrincipal.getUserId();
        roomService.deleteRoom(userId, roomId);
        return ApiResponse.ok("채팅방을 나갔습니다.", null);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody RoomEditRequest roomEditRequest
    ){
        Long userId = userPrincipal.getUserId();
        roomService.editChatRoom(userId, roomEditRequest);
        return ApiResponse.ok("채팅방을 수정했습니다.", null);
    }

    @PatchMapping("/friend")
    public ResponseEntity<ApiResponse<RoomResponse>> addFriend(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody RoomEditRequest roomEditRequest
    ){
        Long userId = userPrincipal.getUserId();
        roomService.inviteFriends(userId, roomEditRequest.getRoomId(), roomEditRequest.getUserIds());
        return ApiResponse.ok("유저를 초대했습니다.", null);
    }

    @GetMapping("/friend/{roomId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getInvitableFriends(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "roomId")  Long roomId
    ){
        Long userId = userPrincipal.getUserId();
        List<UserResponse> response = roomService.getUsersInvitable(userId, roomId);
        return ApiResponse.ok("해당 채팅방에 초대할 수 있는 친구목록을 조회했습니다.", response);
    }

    @GetMapping("/{roomId}/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(name = "roomId") Long roomId
    ){
        Long userId = userPrincipal.getUserId();
        List<UserResponse> response = roomService.getUsersInRoom(userId, roomId);
        return ApiResponse.ok("채팅방의 유저 프로필을 불러왔습니다.", response);
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<RoomInfoResponse>> getRoomInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "roomId") Long roomId
    ){
        RoomInfoResponse response = roomService.getRoomInfo(roomId);
        return ApiResponse.ok("해당 채팅방 정보를 조회했습니다.", response);
    }

    @GetMapping("/edit/info")
    public ResponseEntity<ApiResponse<RoomInfoResponse>> getRoomInfoByEdit(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "roomId") Long roomId
    ){
        RoomInfoResponse response = roomService.getRoomInfoByEdit(roomId);
        return ApiResponse.ok("해당 채팅방 정보를 조회했습니다.", response);
    }
}
