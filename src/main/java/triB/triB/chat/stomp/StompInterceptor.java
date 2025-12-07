package triB.triB.chat.stomp;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.auth.repository.UserRepository;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.security.JwtProvider;
import triB.triB.global.security.UserPrincipal;
import triB.triB.room.repository.UserRoomRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try{
            // Principal null로 주입되는 문제 해결 미친거
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String jwtToken = accessor.getFirstNativeHeader("Authorization");
                if (jwtToken == null || !jwtToken.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("인증 토큰이 필요합니다.");
                }
                String token = jwtToken.substring(7);

                if (!jwtProvider.validateAccessToken(token)) {
                    throw new JwtException("유효하지 않는 토큰입니다.");
                }
                Long userId = jwtProvider.extractUserId(token);

                User user = userRepository.findById(userId)
                        .orElseThrow(()->new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

                if (user.getUserStatus() == UserStatus.DELETED)
                    throw new CustomException(ErrorCode.INVALID_ACCESS);

                UserPrincipal userPrincipal = new UserPrincipal(user, user.getUserId(), user.getEmail(), user.getUsername(), user.getNickname());
                UsernamePasswordAuthenticationToken authentication
                        = new UsernamePasswordAuthenticationToken(userPrincipal, null, null);

                accessor.setUser(authentication); // Principal 설정
                accessor.setLeaveMutable(true);

                log.debug("WebSocket 연결 성공: userId= {}, principal={}", userId, authentication.getName());
            }
            else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                log.debug("채팅룸 확인전");
                if (destination == null || !destination.startsWith("/sub/chat/"))
                    throw new IllegalArgumentException("잘못된 요청입니다.");
                log.debug("채팅룸 확인 중");

                try {
                    Long roomId = Long.parseLong(destination.substring("/sub/chat/".length()));
                    log.debug("roomId={}", roomId);

                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth == null)
                        throw new BadCredentialsException("인증 정보가 없습니다.");
                    UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                    Long userId = userPrincipal.getUserId();

                    log.debug("SUBSCRIBE - subscriptionId= {}, userId={}, roomId={}", accessor.getSubscriptionId(), userId, roomId);
                    boolean hasAccess = userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId);
                    if (!hasAccess) {
                        log.error("채팅방 구독 권한 없음. userId={}, roomId={}", userId, roomId);
                        throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다.");
                    }
                    // 구독 정보 저장 (UNSUBSCRIBE 시 사용)
                    accessor.getSessionAttributes().put("subscription:" + accessor.getSubscriptionId(), roomId);

                    log.debug("채팅방 구독 성공. userId={}, roomId={}", userId, roomId);
                } catch (NumberFormatException e) {
                    log.error("잘못된 roomId 형식: {}", destination);
                    throw new IllegalArgumentException("잘못된 채팅방 ID 형식입니다.");
                }
            }
            else if (StompCommand.SEND.equals(accessor.getCommand())) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth == null)
                    throw new BadCredentialsException("인증 정보가 없습니다.");
                UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                Long userId = userPrincipal.getUserId();

                String destination = accessor.getDestination();

                log.debug("메시지 전송 요청 확인: destination={}", destination);
                if (destination == null || !destination.startsWith("/pub/chat/"))
                    throw new IllegalArgumentException("잘못된 요청입니다.");

                // /pub/chat/{roomId}/send 또는 /pub/chat/{roomId} 형식에서 roomId 추출
                String pathAfterPrefix = destination.substring("/pub/chat/".length());
                String roomIdStr = pathAfterPrefix.contains("/")
                        ? pathAfterPrefix.substring(0, pathAfterPrefix.indexOf("/"))
                        : pathAfterPrefix;
                Long roomId = Long.parseLong(roomIdStr);

                log.debug("SEND - userId={}, roomId={}", userId, roomId);

                boolean hasAccess = userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId);
                if (!hasAccess) {
                    log.error("채팅방 전송 권한 없음. userId={}, roomId={}", userId, roomId);
                    throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다.");
                }
                log.debug("메시지 전송 권한 확인 완료. userId={}, roomId={}", userId, roomId);
            }
            // 채팅룸 구독 해제
            else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth == null)
                    throw new BadCredentialsException("인증 정보가 없습니다.");

                UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                Long userId = userPrincipal.getUserId();

                String subscriptionId = accessor.getSubscriptionId();
                Long roomId = (Long) accessor.getSessionAttributes().get("subscription:" + subscriptionId);

                if (roomId != null) {
                    boolean hasAccess = userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId);
                    if (!hasAccess) {
                        log.error("채팅방 구독 해제 권한 없음. userId={}, roomId={}", userId, roomId);
                        throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다.");
                    }
                    log.debug("UNSUBSCRIBE 권한 검증 완료: userId={}, roomId={}", userId, roomId);
                } else {
                    log.warn("UNSUBSCRIBE 시 roomId가 없음: subscriptionId={}", subscriptionId);
                }
            }
            // 앱 종료시 웹소켓 연결 종료
            else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth != null){
                    UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                    log.debug("WebSocket 연결 해제: userId={}", userPrincipal.getUserId());
                }
            }
            return message;
        } catch (Exception e) {
            log.error("STOMP 메시지 처리 중 에러 발생: {}", e.getMessage(), e);
            return null;  // 메시지 처리 중단
        }
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        ChannelInterceptor.super.postSend(message, channel, sent);
    }
}