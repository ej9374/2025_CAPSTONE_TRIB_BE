package triB.triB.friendship.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.IsAlarm;
import triB.triB.auth.entity.Token;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.friendship.dto.FriendRequest;
import triB.triB.friendship.dto.NewUserResponse;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.friendship.entity.Friend;
import triB.triB.friendship.entity.Friendship;
import triB.triB.friendship.entity.FriendshipStatus;
import triB.triB.friendship.repository.FriendRepository;
import triB.triB.friendship.repository.FriendshipRepository;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.fcm.FcmSendRequest;
import triB.triB.global.fcm.FcmSender;
import triB.triB.global.fcm.RequestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendshipService {

    @Value("${triB-logo}")
    private String tribImage;

    private final FriendshipRepository friendshipRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final FcmSender fcmSender;
    private final TokenRepository tokenRepository;

    public List<UserResponse> getMyFriends(Long userId){

        List<User> friends = friendRepository.findAllFriendByUserAndUserStatus(userId, UserStatus.ACTIVE);
        List<UserResponse> result = new ArrayList<>();

        friends.forEach(friend -> {
            UserResponse u = new UserResponse(friend.getUserId(), friend.getNickname(), friend.getPhotoUrl());
            result.add(u);
        });
        return result;
    }

    public UserResponse getMyProfile(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저가 존재하지 않습니다."));

        return new UserResponse(
                user.getUserId(),
                user.getNickname(),
                user.getPhotoUrl());
    }

    public List<UserResponse> searchMyFriends(Long userId, String nickname){
        List<User> friends = friendRepository.findAllFriendByUserAndFriend_NicknameAndUserStatus(userId, nickname, UserStatus.ACTIVE);
        List<UserResponse> result = new ArrayList<>();

        friends.forEach(friend -> {
            UserResponse u = new UserResponse(friend.getUserId(), friend.getNickname(), friend.getPhotoUrl());
            result.add(u);
        });
        return result;
    }

    public NewUserResponse searchNewFriend(Long userId, String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("해당 아이디의 유저가 존재하지 않습니다."));

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("로그인한 유저의 아이디가 틀렸습니다."));

        if (me.equals(user)) {
            return null;
        }

        boolean isFriend = friendRepository.existsByUser_UserIdAndFriend_UserId(userId, user.getUserId());

        return new NewUserResponse(
                user.getUserId(),
                user.getNickname(),
                user.getPhotoUrl(),
                isFriend
        );
    }

    // 친구 요청 보내기
    @Transactional
    public void requestFriendshipToUser(Long userId1, Long userId2) throws FirebaseMessagingException {
        User requester = userRepository.findById(userId1)
                .orElseThrow(() -> new EntityNotFoundException("친구 요청을 보내는 유저가 존재하지 않습니다."));

        User addressee = userRepository.findById(userId2)
                .orElseThrow(() -> new EntityNotFoundException("친구 요청을 보낼 유저가 존재하지 않습니다."));

        Friendship f1 = friendshipRepository.findByRequester_UserIdAndAddressee_UserIdAndFriendshipStatus(userId1, userId2, FriendshipStatus.REJECTED);
        Friendship f2 = friendshipRepository.findByRequester_UserIdAndAddressee_UserIdAndFriendshipStatus(userId2, userId1, FriendshipStatus.REJECTED);

        if (f1 != null) {
            friendshipRepository.delete(f1);
        }

        if (f2 != null) {
            friendshipRepository.delete(f2);
        }

        if (friendshipRepository.existsByRequester_UserIdAndAddressee_UserId(userId1, userId2)) {
            throw new CustomException(ErrorCode.CONFLICT_FRIENDSHIP_TO_OTHER);
        }

        // requester가 상대고, addressee가 나인 경우 = 이미 유저가 나에게 친구추가를 보낸 경우
        if (friendshipRepository.existsByRequester_UserIdAndAddressee_UserId(userId2, userId1))
            throw new CustomException(ErrorCode.CONFLICT_FRIENDSHIP_TO_ME);

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .friendshipStatus(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        //FCM 메세지 알림 보내기
        log.info("push 알림을 전송합니다.");
        Token token = tokenRepository.findByUser_UserIdAndUser_IsAlarm(friendship.getAddressee().getUserId(), IsAlarm.ON);
        if (token != null) {
            FcmSendRequest fcmSendRequest = sendPushToToken(RequestType.FRIEND_REQUEST, requester.getNickname()+" 님이 나에게 친구를 신청했어요!", token.getToken());
            fcmSender.sendPushNotification(fcmSendRequest);
        }
    }

    // 내게 온 요청 확인
    public List<FriendRequest> getMyRequests(Long userId){
        List<Friendship> requests = friendshipRepository.findAllByAddressee_UserIdAndFriendshipStatusAndUserStatusOrderByCreatedAtAsc(userId, FriendshipStatus.PENDING, UserStatus.ACTIVE);
        List<FriendRequest> result = new ArrayList<>();

        requests.forEach(friendship -> {
            User requester = friendship.getRequester();
            FriendRequest f = new FriendRequest(friendship.getFriendshipId(), requester.getUserId(), requester.getPhotoUrl(), requester.getNickname());
            result.add(f);
        });
        return result;
    }

    // 내게 온 친구요청 수락
    @Transactional
    public void acceptMyFriendship(Long userId, Long friendshipId) throws FirebaseMessagingException {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new EntityNotFoundException("해당 요청이 존재하지 않습니다."));

        if (!userId.equals(friendship.getAddressee().getUserId()))
            throw new BadCredentialsException("해당 요청을 수락할 수 없습니다.");

        friendship.setFriendshipStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        Friend friend1 = Friend.builder()
                .user(friendship.getRequester())
                .friend(friendship.getAddressee())
                .build();

        Friend friend2 = Friend.builder()
                .user(friendship.getAddressee())
                .friend(friendship.getRequester())
                .build();

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        // FCM 메세지 알림 보내기
        log.info("push 알림을 전송합니다.");
        Token token = tokenRepository.findByUser_UserIdAndUser_IsAlarm(friendship.getRequester().getUserId(), IsAlarm.ON);
        if (token != null) {
            FcmSendRequest fcmSendRequest = sendPushToToken(RequestType.FRIEND_ACCEPTED, friendship.getAddressee().getNickname()+" 님과 친구가 되었어요!", token.getToken());
            fcmSender.sendPushNotification(fcmSendRequest);
        }
    }

    // 내게 온 친구요청 거절
    @Transactional
    public void rejectMyFriendship(Long userId, Long friendshipId){
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new EntityNotFoundException("해당 요청이 존재하지 않습니다."));

        if (!userId.equals(friendship.getAddressee().getUserId()))
            throw new BadCredentialsException("해당 요청을 거절할 수 없습니다.");

        friendship.setFriendshipStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    private FcmSendRequest sendPushToToken(RequestType requestType, String content, String token) {
        return FcmSendRequest.builder()
                .requestType(requestType)
                .id(0L)
                .title("TriB")
                .content(content)
                .image(null)
                .token(token)
                .build();
    }
}
