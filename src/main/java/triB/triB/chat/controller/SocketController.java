package triB.triB.chat.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.ErrorResponse;
import triB.triB.chat.dto.*;
import triB.triB.chat.service.SocketService;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.JwtProvider;
import triB.triB.global.security.UserPrincipal;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SocketController {

    private final SocketService socketService;
    private final JwtProvider jwtProvider;
    private final SimpMessagingTemplate messagingTemplate;

    // client가 메세지 전송
    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            Principal principal,
            @DestinationVariable Long roomId,
            @Payload MessageContentRequest messageContentRequest){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Long userId = userPrincipal.getUserId();
        MessageResponse result = socketService.sendMessageToRoom(userId, roomId, messageContentRequest.getContent());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("메세지를 전송했습니다.", result)
        );
    }

    // 메세지 답장
    @MessageMapping("/chat/{roomId}/reply")
    public void replyMessage(
            Principal principal,
            @DestinationVariable Long roomId,
            @Payload MessageReplyRequest messageReplyRequest
    ){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Long userId = userPrincipal.getUserId();
        MessageResponse result = socketService.replyMessageToRoom(userId, roomId, messageReplyRequest.getContent(), messageReplyRequest.getMessageId());
        messagingTemplate.convertAndSend(
                "/sub/chat/"+roomId,
                ApiResponse.success("메세지를 답장했습니다.", result)
        );
    }

    // 지도에서 장소 공유하기
    @MessageMapping("/chat/{roomId}/map/send")
    public void sendMapMessage(
            Principal principal,
            @DestinationVariable Long roomId,
            @Payload PlaceRequest placeRequest
    ){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Long userId = userPrincipal.getUserId();
        MessageResponse result = socketService.sendMapMessageToRoom(userId, roomId, placeRequest.getPlaceId(), placeRequest.getDisplayName(), placeRequest.getLatitude(), placeRequest.getLongitude(), placeRequest.getPhotoUrl());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("장소를 공유했습니다.", result)
        );
    }

    // 채팅방 내에 일정 공유하기
    @MessageMapping("/chat/{roomId}/trip-share")
    public void sendTripShare(
            Principal principal,
            @DestinationVariable Long roomId,
            @Payload MessageCommunityRequest messageCommunityRequest
    ){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Long userId = userPrincipal.getUserId();
        MessageResponse result = socketService.shareCommunityTripPost(userId, roomId, messageCommunityRequest.getPostId());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("게시글을 공유했습니다.", result)
        );
    }

    // 북마크 설정하는거 실시간으로 보여야됨
    @MessageMapping("/chat/{roomId}/bookmark")
    public void setMessageBookmark(
            @DestinationVariable Long roomId,
            @Payload MessageIdRequest messageIdRequest)
    {
        MessageResponse result = socketService.setBookmark(messageIdRequest.getMessageId());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("북마크를 업데이트했습니다.", result)
        );
    }

    // 태그 설정하는거 실시간으로 보여야됨
    @MessageMapping("/chat/{roomId}/tag")
    public void setMessageTag(
            @DestinationVariable Long roomId,
            @Payload MessagePlaceRequest messagePlaceRequest)
    {
        MessageResponse result = socketService.setPlaceTag(messagePlaceRequest.getMessageId(), messagePlaceRequest.getPlaceTag());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("장소 태그를 업데이트했습니다", result)
        );
    }

    // 메세지 수정한거 실시간으로 보여야됨
    @MessageMapping("/chat/{roomId}/edit")
    public void editMessage(
            @DestinationVariable Long roomId,
            @Payload MessageEditRequest messageEditRequest)
    {
        MessageResponse result = socketService.editMessage(messageEditRequest.getMessageId(), messageEditRequest.getContent());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("메세지를 수정했습니다.", result)
        );
    }

    // 메세지 삭제한 것도 실시간으로 보여야됨
    @MessageMapping("/chat/{roomId}/delete")
    public void deleteMessage(
            @DestinationVariable Long roomId,
            @Payload MessageIdRequest messageIdRequest)
    {
        MessageResponse result = socketService.deleteMessage(messageIdRequest.getMessageId());
        messagingTemplate.convertAndSend(
                "/sub/chat/" + roomId,
                ApiResponse.success("메세지를 삭제했습니다.", result)
        );
    }
}