package triB.triB.user.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import triB.triB.auth.dto.UnlinkResponse;
import triB.triB.auth.entity.*;
import triB.triB.auth.repository.OauthAccountRepository;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.auth.security.AppleClientSecretGenerator;
import triB.triB.global.infra.AwsS3Client;
import triB.triB.global.infra.RedisClient;
import triB.triB.global.utils.CheckBadWordsUtil;
import triB.triB.user.dto.MyProfile;
import triB.triB.user.dto.UpdateProfileRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    private final AwsS3Client s3Client;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisClient redisClient;
    private final TokenRepository tokenRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final CheckBadWordsUtil checkBadWordsUtil;
    private final @Qualifier("kakaoWebClient") WebClient kakaoWebClient;
    private final @Qualifier("appleWebClient") WebClient appleWebClient;

    public MyProfile getMyProfile(Long userId) {
        log.info("userId = {}의 프로필", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        return new MyProfile(
                user.getNickname(),
                user.getUsername(),
                user.getPhotoUrl(),
                user.getIsAlarm()
        );
    }

    @Transactional
    public void updateMyProfile(Long userId, MultipartFile photo, UpdateProfileRequest updateProfileRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        if (updateProfileRequest != null){
            if (Boolean.TRUE.equals(updateProfileRequest.getIsDeleted())) {
                if (user.getPhotoUrl() != null) {
                    s3Client.delete(user.getPhotoUrl());
                }
                user.setPhotoUrl(null);
            }
            String nickname = updateProfileRequest.getNickname();
            if (nickname != null && !nickname.isEmpty()) {
                checkBadWordsUtil.validateNoBadWords(nickname);
                log.info("userId = {} 의 닉네임을 변경합니다.", userId);
                user.setNickname(nickname);
            }
        }
        if (photo != null && !photo.isEmpty()){
            log.info("userId = {} 의 프로필 이미지를 변경합니다.", userId);
            if (user.getPhotoUrl() != null) {
                s3Client.delete(user.getPhotoUrl());
            }
            String newPhoto = s3Client.uploadFile(photo);
            user.setPhotoUrl(newPhoto);
        }
        userRepository.save(user);
    }

    @Transactional
    public IsAlarm updateAlarm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        IsAlarm setting = user.getIsAlarm() == IsAlarm.ON ? IsAlarm.OFF : IsAlarm.ON;
        user.setIsAlarm(setting);
        userRepository.save(user);
        log.info("userId = {} 의 알람을 변경합니다.", userId);
        return user.getIsAlarm();
    }

    @Transactional
    public void checkPassword(Long userId, String password) {
        log.info("userId = {} 의 비밀번호를 확인합니다.", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        if (oauthAccountRepository.findByUser_UserId(userId) != null) {
            throw new RuntimeException("소셜 로그인으로 가입한 유저입니다.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new BadCredentialsException("비밀번호가 올바르지 않습니다.");
        }
    }

    @Transactional
    public void updatePassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        log.info("userId = {} 의 비밀번호를 변경합니다.", userId);
    }

    @Transactional
    public void deleteUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        OauthAccount account = oauthAccountRepository.findByUser_UserId(userId);
        if (account  != null) {
            log.info("oauth 존재 확인 완료");
            try {
                if (account.getProvider().equals("kakao")) {
                    log.info("kakao와 연결 끊기");
                    unlinkKakao(account.getProviderUserId());
                }
                if (account.getProvider().equals("apple")) {
                    log.info("apple과 연결 끊기");
                    if (account.getRefreshToken() != null) {
                        unlinkApple(account.getRefreshToken());
                    }
                }
            } catch (Exception e) {
                // ★ 중요: 소셜 연동 해제가 실패하더라도, 우리 DB에서 회원은 삭제되어야 함
                log.error("소셜 연동 해제 실패 (탈퇴는 계속 진행됩니다). provider: {}, error: {}", account.getProvider(), e.getMessage());
            }
            oauthAccountRepository.delete(account);
        }

        tokenRepository.findByUser_UserId(userId).ifPresent(tokenRepository::delete);

        redisClient.deleteData("rf", String.valueOf(userId));
        try {
            if (user.getPhotoUrl() != null)
                s3Client.delete(user.getPhotoUrl());
        } catch(Exception e){
            log.error("프로필 이미지 삭제 실패: {}", e.getMessage());
        }
        user.setUserStatus(UserStatus.DELETED);
        user.setNickname("(탈퇴한 사용자)");
        user.setEmail(null);
        user.setUsername(null);
        user.setPhotoUrl(null);
        userRepository.save(user);
        log.info("user 상태변경 완료");

        log.info("userId = {} 인 유저가 탈퇴했습니다.", userId);
    }

    public Map<String, Object> getMyUsername(Long userId){
        Map<String, Object> map = new HashMap<>();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        String username = user.getUsername();
        map.put("username", username);
        return map;
    }

    @Transactional
    public void saveToken(Long userId, String token) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

            tokenRepository.findByUser_UserId(userId)
                    .ifPresentOrElse(
                            t -> {
                                t.setToken(token);
                                tokenRepository.save(t);
                            },
                            () -> {
                                Token t = Token.builder()
                                        .user(user)
                                        .token(token)
                                        .build();
                                tokenRepository.save(t);
                            }
                    );
        } catch (DataIntegrityViolationException e){
            tokenRepository.findByUser_UserId(userId)
                    .ifPresent(t -> t.setToken(token));
        }
    }

    @Transactional
    public void logout(Long userId) {
        tokenRepository.findByUser_UserId(userId)
                .ifPresent(tokenRepository::delete);
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
                        .fromFormData("client_id", clientId) // app bundle id (예: com.example.app)
                        .with("client_secret", clientSecret) // 아래 설명 참조 (ES256 서명된 JWT)
                        .with("token", refreshToken) // 유저의 리프레시 토큰
                        .with("token_type_hint", "refresh_token"))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
