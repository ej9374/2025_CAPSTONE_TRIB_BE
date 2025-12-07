package triB.triB.chat.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.chat.dto.*;
import triB.triB.chat.entity.*;
import triB.triB.chat.event.ChatMessageCreatedEvent;
import triB.triB.chat.repository.MessageBookmarkRepository;
import triB.triB.chat.repository.MessagePlaceDetailRepository;
import triB.triB.chat.repository.MessagePlaceRepository;
import triB.triB.chat.repository.MessageRepository;
import triB.triB.community.entity.Post;
import triB.triB.community.repository.PostImageRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.room.entity.Room;
import triB.triB.room.entity.RoomReadState;
import triB.triB.room.entity.RoomReadStateId;
import triB.triB.room.entity.UserRoom;
import triB.triB.room.repository.RoomReadStateRepository;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;
    private final MessageBookmarkRepository messageBookmarkRepository;
    private final MessagePlaceRepository messagePlaceRepository;
    private final MessagePlaceDetailRepository messagePlaceDetailRepository;
    private final RoomReadStateRepository roomReadStateRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher publisher;
    private final PostImageRepository postImageRepository;


    // 메세지 전송
    @Transactional
    public MessageResponse sendMessageToRoom(Long userId, Long roomId, String content){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        Message message = Message.builder()
                .room(room)
                .user(user)
                .messageType(MessageType.TEXT)
                .messageStatus(MessageStatus.ACTIVE)
                .content(content)
                .build();
        messageRepository.save(message);

        publisher.publishEvent(new ChatMessageCreatedEvent(
                roomId, userId, user.getNickname(), user.getPhotoUrl(), message.getContent(), message.getMessageType()
        ));

        return MessageResponse.builder()
                .actionType(ActionType.NEW_MESSAGE)
                .user(new UserResponse(userId, user.getNickname(), user.getPhotoUrl()))
                .message(
                        MessageDto.builder()
                                .messageId(message.getMessageId())
                                .content(message.getContent())
                                .messageType(message.getMessageType())
                                .messageStatus(MessageStatus.ACTIVE)
                                .tag(null)
                                .isBookmarked(false)
                                .placeDetail(null)
                                .communityDetail(null)
                                .replyMessage(null)
                                .build()
                )
                .createdAt(message.getCreatedAt())
                .build();
    }

    //메세지 답장
    @Transactional
    public MessageResponse replyMessageToRoom(Long userId, Long roomId, String content, Long messageId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        Message replyMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("해당 메세지가 존재하지 않습니다."));

        // 삭제된 메세지에 답장 불가
        if (replyMessage.getMessageStatus().equals(MessageStatus.DELETE))
            throw new CustomException(ErrorCode.MESSAGE_DELETED);

        Message message = Message.builder()
                .room(room)
                .user(user)
                .messageType(MessageType.TEXT)
                .messageStatus(MessageStatus.ACTIVE)
                .content(content)
                .replyMessage(replyMessage)
                .build();
        messageRepository.save(message);

        publisher.publishEvent(new ChatMessageCreatedEvent(
                roomId, userId, user.getNickname(), user.getPhotoUrl(), message.getContent(), message.getMessageType()
        ));

        return MessageResponse.builder()
                .actionType(ActionType.MESSAGE_REPLY)
                .user(new UserResponse(userId, user.getNickname(), user.getPhotoUrl()))
                .message(
                        MessageDto.builder()
                                .messageId(message.getMessageId())
                                .content(message.getContent())
                                .messageType(message.getMessageType())
                                .messageStatus(MessageStatus.ACTIVE)
                                .tag(null)
                                .isBookmarked(false)
                                .placeDetail(null)
                                .communityDetail(null)
                                .replyMessage(new ReplyMessage(replyMessage.getMessageId(), replyMessage.getContent()))
                                .build()
                )
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 장소 공유
    @Transactional
    public MessageResponse sendMapMessageToRoom(Long userId, Long roomId, String placeId, String displayName, Double latitude, Double longitude, String photoUrl){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        Message message = Message.builder()
                .room(room)
                .user(user)
                .messageType(MessageType.MAP)
                .messageStatus(MessageStatus.ACTIVE)
                .content(null)
                .build();
        messageRepository.save(message);

        MessagePlaceDetail messagePlaceDetail = MessagePlaceDetail.builder()
                .message(message)
                .placeId(placeId)
                .displayName(displayName)
                .latitude(latitude)
                .longitude(longitude)
                .photoUrl(photoUrl)
                .build();
        messagePlaceDetailRepository.save(messagePlaceDetail);

        message.setContent(messagePlaceDetail.getDisplayName());
        messageRepository.save(message);

        publisher.publishEvent(new ChatMessageCreatedEvent(
                roomId, userId, user.getNickname(), user.getPhotoUrl(), message.getContent(), message.getMessageType()
        ));

        return MessageResponse.builder()
                .actionType(ActionType.NEW_MAP_MESSAGE)
                .user(new UserResponse(userId, user.getNickname(), user.getPhotoUrl()))
                .message(
                        MessageDto.builder()
                                .messageId(message.getMessageId())
                                .content(message.getContent())
                                .messageType(message.getMessageType())
                                .messageStatus(MessageStatus.ACTIVE)
                                .tag(null)
                                .isBookmarked(false)
                                .placeDetail(makePlaceDetail(message.getMessageId()))
                                .communityDetail(null)
                                .replyMessage(null)
                                .build()
                )
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 커뮤니티 게시글 공유
    @Transactional
    public MessageResponse shareCommunityTripPost(Long userId, Long roomId, Long postId){
        log.info("일정 공유 요청");
        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId)) {
            throw new BadCredentialsException("해당 채팅방에 보낼 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        Message message = Message.builder()
                .room(room)
                .user(user)
                .messageType(MessageType.COMMUNITY_SHARE)
                .messageStatus(MessageStatus.ACTIVE)
                .content(postId.toString())
                .build();
        messageRepository.save(message);

        log.info("일정 공유 메세지 저장 완료");

        publisher.publishEvent(new ChatMessageCreatedEvent(
                roomId, userId, user.getNickname(), user.getPhotoUrl(), "커뮤니티 게시글을 공유했습니다.", message.getMessageType()
        ));

        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));

        String imageUrl = postImageRepository.findImageUrlByPostId(postId);

        return MessageResponse.builder()
                .actionType(ActionType.NEW_COMMUNITY_SHARE)
                .user(new UserResponse(userId, user.getNickname(), user.getPhotoUrl()))
                .message(
                        MessageDto.builder()
                        .messageId(message.getMessageId())
                        .content(message.getContent())
                        .messageType(MessageType.COMMUNITY_SHARE)
                        .tag(null)
                        .isBookmarked(null)
                        .placeDetail(null)
                        .communityDetail(new CommunityDetail(postId, p.getTitle(), imageUrl))
                        .replyMessage(null)
                        .build()
                )
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 북마크 설정
    @Transactional
    public MessageResponse setBookmark(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("해당 메세지가 존재하지 않습니다."));

        MessageDto messageDto = MessageDto.builder()
                .messageId(messageId)
                .content(null)
                .messageType(null)
                .messageStatus(null)
                .tag(null)
                .placeDetail(null)
                .communityDetail(null)
                .replyMessage(null)
                .build();

        MessageBookmark messageBookmark = messageBookmarkRepository.findByMessage_MessageId(messageId);

        if (messageBookmark == null) {
            MessageBookmark bookmark = MessageBookmark.builder()
                    .room(message.getRoom())
                    .message(message)
                    .content(message.getContent())
                    .build();
            messageBookmarkRepository.save(bookmark);
            messageDto.setIsBookmarked(true);
        } else {
            messageBookmarkRepository.delete(messageBookmark);
            messageDto.setIsBookmarked(false);
        }

        return MessageResponse.builder()
                .actionType(ActionType.BOOKMARK_UPDATE)
                .user(null)
                .message(messageDto)
                .createdAt(null)
                .build();
    }

    // 장소 태그 설정
    @Transactional
    public MessageResponse setPlaceTag(Long messageId, PlaceTag placeTag) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("해당 메세지가 존재하지 않습니다."));

        if (message.getMessageType().equals(MessageType.TEXT)) {
            throw new IllegalArgumentException("해당 타입의 메세지에는 장소 태그를 지정할 수 없습니다.");
        }

        MessageDto messageDto = MessageDto.builder()
                .messageId(messageId)
                .content(null)
                .messageType(null)
                .messageStatus(null)
                .isBookmarked(null)
                .placeDetail(null)
                .communityDetail(null)
                .replyMessage(null)
                .build();

        MessagePlace messagePlace = messagePlaceRepository.findByMessage_MessageId(messageId);

        if (messagePlace == null) {
            MessagePlace m = MessagePlace.builder()
                    .room(message.getRoom())
                    .message(message)
                    .placeTag(placeTag)
                    .build();
            messagePlaceRepository.save(m);
            messageDto.setTag(placeTag);
        } else if (!messagePlace.getPlaceTag().equals(placeTag)) {
            messagePlace.setPlaceTag(placeTag);
            messagePlaceRepository.save(messagePlace);
            messageDto.setTag(placeTag);
        } else {
            messagePlaceRepository.delete(messagePlace);
            messageDto.setTag(null);
        }

        return MessageResponse.builder()
                .actionType(ActionType.TAG_UPDATE)
                .user(null)
                .message(messageDto)
                .createdAt(null)
                .build();
    }

    // 메세지 수정 -> content랑 messageStatus만 조회
    @Transactional
    public MessageResponse editMessage(Long messageId, String content) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("해당 메세지가 존재하지 않습니다."));

        if (content == null) {
            throw new IllegalArgumentException("메세지가 비어있습니다.");
        }

        message.setMessageStatus(MessageStatus.EDIT);
        message.setContent(content);
        message.setUpdatedAt(LocalDateTime.now());
        messageRepository.save(message);

        MessageDto messageDto = MessageDto.builder()
                .messageId(messageId)
                .content(content)
                .messageType(null)
                .messageStatus(MessageStatus.EDIT)
                .tag(null)
                .isBookmarked(null)
                .placeDetail(null)
                .communityDetail(null)
                .replyMessage(null)
                .build();

        return MessageResponse.builder()
                .actionType(ActionType.MESSAGE_EDIT)
                .user(null)
                .message(messageDto)
                .createdAt(null)
                .build();
    }

    // 메세지 삭제 -> messageId랑 messageStatus만 조회
    @Transactional
    public MessageResponse deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("해당 메세지가 존재하지 않습니다."));

        message.setMessageStatus(MessageStatus.DELETE);
        message.setContent("삭제된 메세지입니다.");
        message.setUpdatedAt(LocalDateTime.now());
        messageRepository.save(message);

        MessageDto messageDto = MessageDto.builder()
                .messageId(messageId)
                .content(null)
                .messageType(null)
                .messageStatus(MessageStatus.DELETE)
                .tag(null)
                .isBookmarked(null)
                .placeDetail(null)
                .communityDetail(null)
                .replyMessage(null)
                .build();

        return MessageResponse.builder()
                .actionType(ActionType.MESSAGE_DELETE)
                .user(null)
                .message(messageDto)
                .createdAt(null)
                .build();
    }

    @Transactional
    public void saveLastReadMessage(Long userId, Long roomId) {
        Long messageId = messageRepository.findLastReadMessageIdByRoom_RoomId(roomId);
        if (messageId == null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        RoomReadState r = roomReadStateRepository.findByRoom_RoomIdAndUser_UserId(roomId, userId);
        // 만약에 이미 읽은 기록이 있다면 업데이트 없다면 생성
        if (r == null) {
            log.debug("새로운 읽음 상태 저장");
            RoomReadStateId id = new RoomReadStateId(userId, roomId);
            RoomReadState roomReadState = RoomReadState.builder()
                    .id(id)
                    .user(user)
                    .room(room)
                    .lastReadMessageId(messageId)
                    .build();
            roomReadStateRepository.save(roomReadState);
        } else {
            log.debug("읽음 상태 업데이트");
            r.setLastReadMessageId(messageId);
            roomReadStateRepository.save(r);
        }
        log.debug("마지막 읽은 메세지 저장 완료: userId={}, roomId={}, messageId={}", userId, roomId, messageId);
    }

    private PlaceDetail makePlaceDetail(Long messageId) {
        MessagePlaceDetail mpd = messagePlaceDetailRepository.findByMessage_MessageId(messageId);

        if (mpd == null) {
            return null;
        }
        return PlaceDetail.builder()
                .placeId(mpd.getPlaceId())
                .displayName(mpd.getDisplayName())
                .latitude(mpd.getLatitude())
                .longitude(mpd.getLongitude())
                .photoUrl(mpd.getPhotoUrl())
                .build();
    }
}
