package triB.triB.chat.event;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import triB.triB.auth.entity.IsAlarm;
import triB.triB.auth.entity.Token;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.global.fcm.FcmSendRequest;
import triB.triB.global.fcm.FcmSender;
import triB.triB.global.fcm.RequestType;
import triB.triB.global.security.UserPrincipal;
import triB.triB.room.entity.Room;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPushNotifier {
    private final FcmSender fcmSender;
    private final UserRoomRepository userRoomRepository;
    private final TokenRepository tokenRepository;
    private final RoomRepository roomRepository;
    private final SimpUserRegistry simpUserRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(ChatMessageCreatedEvent e){
        try {
            log.info("message push notification send");
            Room room = roomRepository.findById(e.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다"));
            List<User> users = userRoomRepository.findUsersByRoomIdAndIsAlarm(e.roomId(), IsAlarm.ON);
            List<Long> targetUserIds = users.stream()
                    .filter(user -> user.getUserStatus() == UserStatus.ACTIVE)
                    .map(User::getUserId)
                    .filter(id -> !Objects.equals(id, e.userId()))
                    .filter(id -> !isOnlineUsersInRoom(e. roomId(), id))
                    .toList();

            if (targetUserIds.isEmpty()) return;

            List<Token> tokens = tokenRepository.findAllByUser_UserIdInAndUser_IsAlarm(targetUserIds, IsAlarm.ON);

            if (tokens.isEmpty()) return;

            String roomName = room.getRoomName();
            String content = "👤"+ e.nickname() +"\n"+ e.content();
            String image = e.photoUrl();

            for (Token t : tokens) {
                if (t != null) {
                    try {
                        FcmSendRequest fcmSendRequest = FcmSendRequest.builder()
                                .requestType(RequestType.MESSAGE)
                                .id(e.roomId()) //roomId 넣고 클릭하면 글로이동
                                .title(roomName)
                                .content(content)
                                .image(image)
                                .token(t.getToken())
                                .build();
                        fcmSender.sendPushNotification(fcmSendRequest);
                    } catch (Exception ex) {
                        log.error("FCM push failed for userId={}, token={}", t.getUser().getUserId(), t.getToken(), ex);
                    }
                }
            }
        } catch  (Exception ex) {
            log.error("FCM push after-commit failed. roomId={}", e.roomId(), ex);
        }
    }

    private boolean isOnlineUsersInRoom(Long roomId, Long userId) {
        List<Long> onlineUserIds = new ArrayList<>();
        String destination = "/sub/chat/" + roomId;

        simpUserRegistry.getUsers().forEach(user -> {
            user.getSessions().forEach(session -> {
                session.getSubscriptions().forEach(subscription -> {
                    if (destination.equals(subscription.getDestination())) {
                        if (user.getPrincipal() instanceof UsernamePasswordAuthenticationToken) {
                            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) user.getPrincipal();
                            if (auth.getPrincipal() instanceof UserPrincipal) {
                                UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                                onlineUserIds.add(userPrincipal.getUserId());
                            }
                        }
                    }
                });
            });
        });
        return onlineUserIds.contains(userId);
    }
}
