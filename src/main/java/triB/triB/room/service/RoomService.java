package triB.triB.room.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.auth.repository.UserRepository;
import triB.triB.chat.entity.MessageStatus;
import triB.triB.chat.entity.MessageType;
import triB.triB.community.repository.PostRepository;
import triB.triB.community.repository.UserBlockRepository;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.friendship.repository.FriendRepository;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.utils.CheckBadWordsUtil;
import triB.triB.room.dto.*;
import triB.triB.chat.entity.Message;
import triB.triB.room.entity.Room;
import triB.triB.room.entity.RoomStatus;
import triB.triB.room.entity.UserRoom;
import triB.triB.chat.repository.MessageRepository;
import triB.triB.room.entity.UserRoomId;
import triB.triB.room.repository.RoomReadStateRepository;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRoomRepository userRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final PostRepository postRepository;
    private final UserBlockRepository userBlockRepository;
    private final CheckBadWordsUtil checkBadWordsUtil;

    @Transactional(readOnly = true)
    public List<RoomsResponse> getRoomList(Long userId){
        List<UserRoom> userRooms = userRoomRepository.findAllWithRoomAndUsersByUser_UserId(userId);
        log.info("로그인한 유저의 room 목록 조회");
        return roomList(userRooms, userId);
    }

    @Transactional(readOnly = true)
    public List<RoomsResponse> searchRoomList(Long userId, String content){
        List<UserRoom> userRooms = userRoomRepository.findAllWithRoomAndUsersByUser_UserIdAndRoom_RoomName(userId, content);
        log.info("로그인한 유저가 검색하는 room 목록 조회");
        return roomList(userRooms, userId);
    }

    public List<ChatUserResponse> selectFriends(List<Long> userIds) {
        List<ChatUserResponse> responses = new ArrayList<>();
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
            log.info("nickname = {}, photoUrl = {}", user.getNickname(), user.getPhotoUrl());
            ChatUserResponse u = new ChatUserResponse(user.getNickname(), user.getPhotoUrl());
            responses.add(u);
        }
        return responses;
    }

    @Transactional
    public RoomResponse makeChatRoom(Long userId, RoomRequest roomRequest) {
        Set<Long> blockedUserIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerUserId(userId));

        checkBadWordsUtil.validateNoBadWords(roomRequest.getRoomName());

        Room room = Room.builder()
                .roomName(roomRequest.getRoomName())
                .destination(roomRequest.getCountry())
                .startDate(roomRequest.getStartDate())
                .endDate(roomRequest.getEndDate())
                .build();

        roomRepository.save(room);

        log.info("방 생성완료 roomId = {}", room.getRoomId());

        List<Long> userIds = roomRequest.getUserIds();
        userIds.add(userId);

        log.info("userIds = {}", userIds);

        for (Long id : userIds) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("유저가 존재하지 않습니다."));
            if (blockedUserIds.contains(id)) {
                throw new CustomException(ErrorCode.INVITE_BLOCKED);
            }
            if (user.getUserStatus() == UserStatus.DELETED)
                continue;


            UserRoom userRoom = UserRoom.builder()
                    .id(new UserRoomId(user.getUserId(), room.getRoomId()))
                    .user(user)
                    .room(room)
                    .roomStatus(RoomStatus.ACTIVE)
                    .build();

            userRoomRepository.save(userRoom);
        }

        return new RoomResponse(room.getRoomId());
    }

    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        UserRoom userRoom = userRoomRepository.findByUser_UserIdAndRoom_RoomId(userId, roomId);
        if (userRoom == null) {
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다");
        }
        userRoom.setRoomStatus(RoomStatus.EXIT);
        userRoomRepository.save(userRoom);
    }

    @Transactional
    public void editChatRoom(Long userId, RoomEditRequest roomRequest) {
        Long roomId = roomRequest.getRoomId();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId)){
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다");
        }

        String country = roomRequest.getCountry();
        String roomName = roomRequest.getRoomName();
        LocalDate startDate = roomRequest.getStartDate();
        LocalDate endDate = roomRequest.getEndDate();

        if (country != null) {
            room.setDestination(roomRequest.getCountry());
        }
        if (roomName != null) {
            checkBadWordsUtil.validateNoBadWords(roomRequest.getRoomName());
            room.setRoomName(roomRequest.getRoomName());
        }
        if (startDate != null) {
            room.setStartDate(startDate);
        }
        if (endDate != null) {
            room.setEndDate(endDate);
        }
        roomRepository.save(room);
    }

    @Transactional
    public void inviteFriends(Long userId, Long roomId, List<Long> userIds) {
        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId)){
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        Set<Long> blockedUserIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerUserId(userId));

        for (Long id : userIds) {
            UserRoom ur;
            if (blockedUserIds.contains(id)) {
                throw new CustomException(ErrorCode.INVITE_BLOCKED);
            }
            if ((ur = userRoomRepository.findByUserIdAndRoomIdWithoutFilter(id, roomId)) != null){
                if (ur.getRoomStatus().equals(RoomStatus.EXIT)){
                    ur.setRoomStatus(RoomStatus.ACTIVE);
                    userRoomRepository.save(ur);
                }
                else {
                    throw new DataIntegrityViolationException("이미 초대된 유저입니다.");
                }
            } else {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
                if (user.getUserStatus() == UserStatus.DELETED)
                    continue;
                UserRoom ur2 = UserRoom.builder()
                        .user(user)
                        .room(room)
                        .roomStatus(RoomStatus.ACTIVE)
                        .build();
                userRoomRepository.save(ur2);
            }
        }
    }

    public List<UserResponse> getUsersInvitable(Long userId, Long roomId) {
        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId)){
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다");
        }
        Set<Long> blockedUserIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerUserId(userId));

        return friendRepository.findAllFriendByUserAndUserStatus(userId, UserStatus.ACTIVE)
                .stream()
                .filter(user -> !userRoomRepository
                        .existsByUser_UserIdAndRoom_RoomId(user.getUserId(), roomId))
                .filter(f -> !blockedUserIds.contains(f.getUserId()))
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .nickname(user.getNickname())
                        .photoUrl(user.getPhotoUrl())
                        .build())
                .toList();
    }

    public List<UserResponse> getUsersInRoom(Long userId, Long roomId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId)){
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다");
        }
        List<UserResponse> responses = new ArrayList<>();
        UserResponse me = new UserResponse(userId, user.getNickname(), user.getPhotoUrl());
        responses.add(me);

        responses.addAll(userRoomRepository.findByRoom_RoomIdAndNotUser_UserIdAndUserStatus(roomId, userId, UserStatus.ACTIVE).stream()
                        .map(userRoom -> {
                            User u = userRoom.getUser();
                            return UserResponse.builder()
                                    .userId(u.getUserId())
                                    .nickname(u.getNickname())
                                    .photoUrl(u.getPhotoUrl())
                                    .build();
                        })
                .toList());
        return responses;
    }

    public RoomInfoResponse getRoomInfo(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        return RoomInfoResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .destination(null)
                .photoUrls(null)
                .build();
    }

    public RoomInfoResponse getRoomInfoByEdit(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        return RoomInfoResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .destination(room.getDestination())
                .photoUrls(userRoomRepository.findAllByRoom_RoomIdAndUserStatus(roomId, UserStatus.ACTIVE))
                .build();
    }

    private List<RoomsResponse> roomList(List<UserRoom> userRooms, Long userId) {
        if (userRooms.isEmpty()) {
            log.info("유저가 참여한 채팅방이 없습니다.");
            return new ArrayList<>();
        }

        // 1. 모든 방 ID를 가져오기
        List<Room> rooms = userRooms.stream()
                .map(UserRoom::getRoom)
                .toList();

        List<Long> roomIds = rooms.stream()
                .map(Room::getRoomId)
                .toList();

        // 2. 모든 방의 사용자 정보를 단일 쿼리로 한번에 가져오기
        List<UserRoom> allUsersInRoom = userRoomRepository.findAllWithUsersByRoomIdsAndUserStatus(roomIds, UserStatus.ACTIVE);

        Map<Long, Integer> peopleCountMap = allUsersInRoom.stream()
                .collect(Collectors.groupingBy(
                        ur -> ur.getId().roomId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<Long, List<String>> roomPhotoMap = allUsersInRoom.stream()
                .collect(Collectors.groupingBy(
                        ur -> ur.getId().roomId(),
                        LinkedHashMap::new,
                        Collectors.mapping(ur -> ur.getUser().getPhotoUrl(),
                                Collectors.toCollection(ArrayList::new))
                ));

        // 3. 모든 방의 마지막 메세지를 한꺼번에 가져오기
        List<Message> lastMessages = messageRepository.findLastMessagesByRooms(roomIds);
        Map<Long, Message> lastMessageMap = lastMessages.stream()
                .collect(Collectors.toMap(
                        msg -> msg.getRoom().getRoomId(),
                        msg -> msg
                ));

        // 5. 읽지 않은 메세지 수를 배치로 가져오기
        Map<Long, Integer> notReadMessageTotalMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            List<Object[]> unreadCounts = messageRepository.countUnreadMessagesBatch(roomIds, userId);
            unreadCounts.forEach(row -> notReadMessageTotalMap.put((Long) row[0], ((Number) row[1]).intValue()));
        }

        // 6. 메모리에서 최종 응답을 구성함
        List<RoomsResponse> responses = new ArrayList<>();
        for (Room r : rooms) {
            Message msg = lastMessageMap.get(r.getRoomId());
            String content = null;
            if (msg != null) {
                if (msg.getMessageStatus() != MessageStatus.DELETE) {
                    if (msg.getMessageType() == MessageType.COMMUNITY_SHARE) {
                        content = postRepository.findTitleByPostId(Long.parseLong(msg.getContent()));
                        if (content == null) {
                            content = "삭제된 게시글입니다.";
                        }
                    } else {
                        content = msg.getContent();
                    }
                } else {
                    // 삭제된 커뮤니티 게시글인 경우 내용을 그대로 사용
                    content = msg.getContent();
                }
            }
            RoomsResponse response = RoomsResponse.builder()
                    .roomId(r.getRoomId())
                    .roomName(r.getRoomName())
                    .photoUrls(roomPhotoMap.getOrDefault(r.getRoomId(), new ArrayList<>()))
                    .destination(r.getDestination())
                    .startDate(r.getStartDate())
                    .endDate(r.getEndDate())
                    .content(content)
                    .createdAt(msg != null ? msg.getCreatedAt() : null)
                    .messageNum(notReadMessageTotalMap.getOrDefault(r.getRoomId(), 0))
                    .people(peopleCountMap.getOrDefault(r.getRoomId(), 0))
                    .build();
            responses.add(response);
        }
        return responses;
    }
}
