package triB.triB.schedule.event;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import triB.triB.room.entity.Room;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulePushNotifier {
    private final FcmSender fcmSender;
    private final UserRoomRepository userRoomRepository;
    private final TokenRepository tokenRepository;
    private final RoomRepository roomRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleBatchUpdated(ScheduleBatchUpdatedEvent e) {
        try {
            log.info("Schedule batch update push notification send. tripId={}, roomId={}", e.tripId(), e.roomId());

            Room room = roomRepository.findById(e.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다"));

            List<User> users = userRoomRepository.findUsersByRoomIdAndIsAlarm(e.roomId(), IsAlarm.ON);
            List<Long> targetUserIds = users.stream()
                    .filter(user -> user.getUserStatus() == UserStatus.ACTIVE)
                    .map(User::getUserId)
                    .filter(id -> !Objects.equals(id, e.userId()))
                    .toList();

            if (targetUserIds.isEmpty()) return;

            List<Token> tokens = tokenRepository.findAllByUser_UserIdInAndUser_IsAlarm(targetUserIds, IsAlarm.ON);

            if (tokens.isEmpty()) return;

            String roomName = room.getRoomName();
            String content = e.nickname() + "님이 " + e.dayNumber() + "일차 일정을 수정했습니다.";

            for (Token t : tokens) {
                if (t != null) {
                    try {
                        FcmSendRequest fcmSendRequest = FcmSendRequest.builder()
                                .requestType(RequestType.SCHEDULE_UPDATED)
                                .id(e.tripId())
                                .title(roomName)
                                .content(content)
                                .image(null)
                                .token(t.getToken())
                                .build();

                        fcmSender.sendPushNotification(fcmSendRequest);
                    } catch (Exception ex) {
                        log.error("FCM push failed for userId={}, token={}", t.getUser().getUserId(), t.getToken(), ex);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("FCM push after-commit failed. tripId={}, roomId={}", e.tripId(), e.roomId(), ex);
        }
    }
}
