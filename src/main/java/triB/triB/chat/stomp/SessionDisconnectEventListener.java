package triB.triB.chat.stomp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import triB.triB.chat.service.SocketService;
import triB.triB.global.security.UserPrincipal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionDisconnectEventListener {

    private final SocketService socketService;

    @EventListener(SessionDisconnectEvent.class)
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null)
            return;

        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Long userId = userPrincipal.getUserId();

        // 남아있는 구독 정보가 있는지 확인
        boolean hasSubscription = accessor.getSessionAttributes().keySet().stream()
                .anyMatch(key -> key.toString().startsWith("subscription:"));

        if (hasSubscription) {
            log.info("DISCONNECT - 남아있는 구독 정보 처리: userId={}", userId);
            accessor.getSessionAttributes().forEach((k, v) -> {
                String keyStr = k.toString();
                if (keyStr.startsWith("subscription:") && v instanceof Long) {
                    Long roomId = (Long) v;
                    try {
                        socketService.saveLastReadMessage(userId, roomId);
                    } catch (Exception e) {
                        log.error("마지막 읽은 메세지 저장 실패: userId={}, roomId={}, error={}", userId, roomId,  e.getMessage());
                    }
                }
            });

            // 구독 정보 제거 (메모리 정리)
            accessor.getSessionAttributes().entrySet().removeIf(entry ->
                    entry.getKey().toString().startsWith("subscription:")
            );
        }
    }
}
