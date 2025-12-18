package triB.triB.friendship.event;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import triB.triB.auth.entity.IsAlarm;
import triB.triB.auth.entity.Token;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.global.fcm.FcmSendRequest;
import triB.triB.global.fcm.FcmSender;
import triB.triB.global.fcm.RequestType;

@Component
@Slf4j
@AllArgsConstructor
public class FriendshipPushNotifier {
    private final FcmSender fcmSender;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendshipCreated(FriendshipEvent e) {
        try {
            log.info("friendship push notification send");
            Token t = null;
            String content = "";
            // 알림은 addresseeId에게 메세지는 requesterId
            if (e.requestType() == RequestType.FRIEND_REQUEST) {
                log.info("친구 요청");
                t = tokenRepository.findByUser_UserIdAndUser_IsAlarm(e.addresseeId(), IsAlarm.ON);
                User requester = userRepository.findById(e.requesterId())
                        .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
                content = requester.getNickname()+" 님이 나에게 친구를 신청했어요!";
            }

            // 알림은 requesterId에게 메세지는 addresseeId
            if (e.requestType() == RequestType.FRIEND_ACCEPTED) {
                log.info("친구 요청 수락");
                t = tokenRepository.findByUser_UserIdAndUser_IsAlarm(e.requesterId(), IsAlarm.ON);
                User addressee = userRepository.findById(e.addresseeId())
                        .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
                content = addressee.getNickname()+" 님과 친구가 되었어요!";
            }

            if (t != null){
                try {
                    FcmSendRequest fcmSendRequest = FcmSendRequest.builder()
                            .requestType(e.requestType())
                            .id(0L)
                            .title("TriB")
                            .content(content)
                            .image(null)
                            .token(t.getToken())
                            .build();
                    fcmSender.sendPushNotification(fcmSendRequest);
                } catch (Exception ex) {
                    log.error("FCM push failed for userId={}, token={}", t.getUser().getUserId(), t.getToken(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("FCM push after-commit failed, requestType = {}, requesterId={}, addresseeId={}", e.requestType(), e.requesterId(), e.addresseeId(), ex);
        }
    }
}
