package triB.triB.chat.event;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class TripPushNotifier {

    private final FcmSender fcmSender;
    private final RoomRepository roomRepository;
    private final TokenRepository tokenRepository;
    private final UserRoomRepository userRoomRepository;

    @EventListener(TripCreatedEvent.class)
    public void onTripCreated(TripCreatedEvent e) {
        try {
            log.info("trip created notification send");
            Room room = roomRepository.findById(e.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

            List<User> users = userRoomRepository.findUsersByRoomIdAndIsAlarm(e.roomId(), IsAlarm.ON);
            List<Long> targetUserIds = users.stream()
                    .filter(user -> user.getUserStatus() == UserStatus.ACTIVE)
                    .map(User::getUserId)
                    .toList();

            if (targetUserIds.isEmpty()) return;

            List<Token> tokens = tokenRepository.findAllByUser_UserIdInAndUser_IsAlarm(targetUserIds, IsAlarm.ON);

            if (tokens.isEmpty()) return;
            String roomName = room.getRoomName();
            for (Token t : tokens) {
                if (t != null) {
                    try {
                        FcmSendRequest fcmSendRequest = FcmSendRequest.builder()
                                .requestType(RequestType.TRIP_CREATED)
                                .id(e.roomId())
                                .title("TriB")
                                .content(roomName + " 에 대한 일정이 성공적으로 생성되었어요!")
                                .image(null)
                                .token(t.getToken())
                                .build();
                        fcmSender.sendPushNotification(fcmSendRequest);
                    } catch (Exception ex) {
                        log.error("FCM push failed for userId={}, token={}", t.getUser().getUserId(), t.getToken(), ex);
                    }
                }
            }
        } catch (Exception ex){
            log.error("FCM push after-commit failed. roomId={}", e.roomId(), ex);
        }
    }

    @EventListener(TripErrorEvent.class)
    public void onTripError(TripErrorEvent e) {
        try {
            log.info("trip created error notification send");
            Room room = roomRepository.findById(e.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

            List<User> users = userRoomRepository.findUsersByRoomIdAndIsAlarm(e.roomId(), IsAlarm.ON);
            List<Long> targetUserIds = users.stream()
                    .filter(user -> user.getUserStatus() == UserStatus.ACTIVE)
                    .map(User::getUserId)
                    .toList();

            if (targetUserIds.isEmpty()) return;

            List<Token> tokens = tokenRepository.findAllByUser_UserIdInAndUser_IsAlarm(targetUserIds, IsAlarm.ON);

            if (tokens.isEmpty()) return;
            String roomName = room.getRoomName();
            for (Token t : tokens) {
                if (t != null) {
                    try {
                        FcmSendRequest fcmSendRequest = FcmSendRequest.builder()
                                .requestType(RequestType.TRIP_ERROR)
                                .id(e.roomId())
                                .title("TriB")
                                .content(roomName + " 의 일정을 만들지 못했어요. 채팅방에서 다시 시도해보세요!")
                                .image(null)
                                .token(t.getToken())
                                .build();
                        fcmSender.sendPushNotification(fcmSendRequest);
                    } catch (Exception ex) {
                        log.error("FCM push failed for userId={}, token={}", t.getUser().getUserId(), t.getToken(), ex);
                    }
                }
            }
        } catch (Exception ex){
            log.error("FCM push after-commit failed. roomId={}", e.roomId(), ex);
        }
    }
}
