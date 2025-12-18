package triB.triB.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import triB.triB.auth.dto.UnlinkResponse;
import triB.triB.auth.security.AppleClientSecretGenerator;
import triB.triB.global.infra.RedisClient;

import java.util.Set;

/**
 * 애플 회원가입 미완료 정리 스케줄러
 *
 * 약관 동의 안 하고 5분이 지나면 애플과의 연결을 해제
 * (애플은 첫 연결 시에만 사용자 정보를 제공하므로 연결 해제 필요)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialSignupCleanupScheduler {

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    private final RedisClient redisClient;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final @Qualifier("kakaoWebClient") WebClient kakaoWebClient;
    private final @Qualifier("appleWebClient") WebClient appleWebClient;

    /**
     * 1분마다 실행: TTL이 60초 미만으로 남은 pending 애플 가입 건들을 처리
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void cleanupExpiredAppleSignups() {
        try {
            Set<String> pendingKeys = redisClient.getPendingKeys();

            if (pendingKeys == null || pendingKeys.isEmpty()) {
                return;
            }

            for (String key : pendingKeys) {
                Long ttl = redisClient.getTimeToLive(key);
                String providerId;
                // TTL이 60초 미만이면 unlink 호출
                if (ttl != null && ttl > 0 && ttl < 60) {
                    if (key.contains("kakao")) {
                        providerId = key.substring("pending:kakao|".length());
                        log.info("카카오 회원가입 미완료 처리 시작. providerId: {}, TTL: {}초", providerId, ttl);
                        try {
                            unlinkKakao(providerId);
                            log.info("카카오 연결 해제 완료. providerId: {}", providerId);
                        } catch (Exception e) {
                            log.error("카카오 연결 해제 실패. providerId: {}, error: {}", providerId, e.getMessage());
                        }
                        // unlink 성공 여부와 관계없이 Redis에서 삭제
                        redisClient.deleteByFullKey(key);
                    }
                    if (key.contains("apple")) {
                        String appleRefreshToken = redisClient.getValueByFullKey(key);
                        providerId = key.substring("pending:apple|".length());

                        if (appleRefreshToken != null) {
                            log.info("애플 회원가입 미완료 처리 시작. providerId: {}, TTL: {}초", providerId, ttl);
                            try {
                                unlinkApple(appleRefreshToken);
                                log.info("애플 연결 해제 완료. providerId: {}", providerId);
                            } catch (Exception e) {
                                log.error("애플 연결 해제 실패. providerId: {}, error: {}", providerId, e.getMessage());
                            }
                        }
                        redisClient.deleteByFullKey(key);
                    }
                }
            }
        } catch (Exception e) {
            log.error("소셜 회원가입 정리 스케줄러 오류: {}", e.getMessage(), e);
        }
    }

    private void unlinkKakao(String providerUserId){
        kakaoWebClient.post()
                .uri("/v1/user/unlink")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("target_id_type", "user_id")
                        .with("target_id", providerUserId))
                .retrieve()
                .bodyToMono(UnlinkResponse.class)
                .block();
    }

    private void unlinkApple(String refreshToken) throws Exception {
        String clientSecret = clientSecretGenerator.generateAppleClientSecret();
        appleWebClient.post()
                .uri("https://appleid.apple.com/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", appleClientId)
                        .with("client_secret", clientSecret)
                        .with("token", refreshToken)
                        .with("token_type_hint", "refresh_token"))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}