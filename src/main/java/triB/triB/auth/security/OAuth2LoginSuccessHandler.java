package triB.triB.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import triB.triB.auth.domain.AppleUserInfo;
import triB.triB.auth.domain.GoogleUserInfo;
import triB.triB.auth.domain.KakaoUserInfo;
import triB.triB.auth.domain.OAuth2UserInfo;
import triB.triB.auth.entity.OauthAccount;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.OauthAccountRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.global.infra.RedisClient;
import triB.triB.global.security.JwtProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 소셜로그인 성공시 자동 실행
 */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${redirect.base-url}")
    private String redirectUrl;

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final OauthAccountRepository oauthAccountRepository;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId().toLowerCase();
        Map<String, Object> attributes = token.getPrincipal().getAttributes();

        OAuth2UserInfo oAuth2UserInfo = switch (provider) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "apple" -> new AppleUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜입니다.");
        };

        String providerId = oAuth2UserInfo.getProviderId();
        String photoUrl = oAuth2UserInfo.getProfileImageUrl();
        String nickname = oAuth2UserInfo.getNickname();
        log.info("provider = " + provider + " providerId = " + providerId + " photoUrl = " + photoUrl);

        String appleRefreshToken = null;
        if (provider.equals("apple")) {
            OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    authentication,
                    request
            );
            OAuth2RefreshToken refreshToken = client.getRefreshToken();
            if (refreshToken != null) {
                appleRefreshToken = refreshToken.getTokenValue();
                log.info("appleRefreshToken 획득 {}", appleRefreshToken);
            }
        }
        Optional<OauthAccount> existUser = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerId);

        String targetUrl;

        if (existUser.isEmpty()) {
            String registerToken = "";
            Map<String, Object> body = new HashMap<>();
            if (provider.equals("apple")) {
                log.info("apple 회원가입입니다. 약관동의를 받아야 합니다.");
                String email = oAuth2UserInfo.getEmail();
                log.info("email = {}", email);
                log.info("nickname = {}", nickname);

                // email이 null인 경우 대비
                String username;
                if (email != null && email.contains("@")) {
                    username = email.split("@")[0] + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
                } else {
                    // email이 없으면 providerId 기반으로 username 생성
                    username = "apple" + providerId.substring(0, Math.min(10, providerId.length()))
                              + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
                }
                log.info("username = {}", username);

                registerToken = jwtProvider.generateRegisterToken(provider, providerId, photoUrl, nickname, username, appleRefreshToken);

                // 약관 동의 안 하고 5분 지나면 스케줄러가 자동으로 애플 연결 해제
                if (appleRefreshToken != null) {
                    redisClient.savePendingSignup("apple", providerId, appleRefreshToken);
                    log.info("애플 pending 데이터 저장 완료. providerId: {}", providerId);
                }

                body.put("isNewUser", true);
                body.put("provider", provider);
                body.put("registerToken", registerToken);

           } else {
                log.info("신규 유저 입니다. 회원가입 페이지로 리디렉션합니다.");
                registerToken = jwtProvider.generateRegisterToken(provider, providerId, photoUrl, nickname, null, null);
                log.info("registerToken = {}", registerToken);

                redisClient.savePendingSignup(provider, providerId, null);

                body.put("isNewUser", true);
                body.put("registerToken", registerToken);
                body.put("photoUrl", photoUrl);
                body.put("nickname", nickname);
            }
            String key = redisClient.setTicketData("ti", objectMapper.writeValueAsString(body));
            log.info("key = {}, body = {}", key, objectMapper.writeValueAsString(body));
            targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("key", key)
                    .build().toString();
        } else {
            log.info("기존 유저 입니다. 메인 페이지로 리디렉션합니다.");
            OauthAccount account = existUser.get();
            User user = existUser.get().getUser();
            Long userId = user.getUserId();

            if (appleRefreshToken != null) {
                account.setRefreshToken(appleRefreshToken);
                oauthAccountRepository.save(account);
            }
            String accessToken = jwtProvider.generateAccessToken(user.getUserId());
            String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

            log.info("accessToken = {}, refreshToken = {}", accessToken, refreshToken);

            Map<String, Object> body = new HashMap<>();
            body.put("isNewUser", false);
            body.put("accessToken", accessToken);
            body.put("refreshToken", refreshToken);
            body.put("userId", userId);

            String key = redisClient.setTicketData("ti", objectMapper.writeValueAsString(body));

            log.info("key = {}, body = {}", key, objectMapper.writeValueAsString(body));

            targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("key", key)
                    .build().toUriString();
        }
        response.sendRedirect(targetUrl);
    }
}

/*
if (provider.equals("apple")) {
                log.info("apple 회원가입입니다. 즉시 회원가입 처리합니다.");
                String email = oAuth2UserInfo.getEmail();
                log.info("email = {}", email);
                log.info("nickname = {}", nickname);
                String username = email.split("@")[0] + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
                log.info("username = {}", username);

                User newUser = User.builder()
                        .email(null)
                        .password(null)
                        .nickname(nickname)
                        .username(username)
                        .photoUrl(null)
                        .build();
                userRepository.save(newUser);

                OauthAccount account = OauthAccount.builder()
                        .user(newUser)
                        .providerUserId(providerId)
                        .provider(provider)
                        .refreshToken(appleRefreshToken)
                        .build();
                oauthAccountRepository.save(account);

                String accessToken = jwtProvider.generateAccessToken(newUser.getUserId());
                String refreshToken = jwtProvider.generateRefreshToken(newUser.getUserId());

                log.info("accessToken = {}, refreshToken = {}", accessToken, refreshToken);

                Map<String, Object> body = new HashMap<>();
                body.put("isNewUser", true);
                body.put("accessToken", accessToken);
                body.put("refreshToken", refreshToken);
                body.put("userId", newUser.getUserId());
                body.put("provider", "apple");
                String key = redisClient.setTicketData("ti", objectMapper.writeValueAsString(body));

                log.info("key = {}, body = {}", key, objectMapper.writeValueAsString(body));

                targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                        .queryParam("key", key)
                        .build().toUriString();

            } else {
                log.info("신규 유저 입니다. 회원가입 페이지로 리디렉션합니다.");
                String registerToken = jwtProvider.generateRegisterToken(provider, providerId, photoUrl, nickname);
                log.info("registerToken = {}", registerToken);

                Map<String, Object> body = new HashMap<>();
                body.put("isNewUser", true);
                body.put("registerToken", registerToken);
                body.put("photoUrl", photoUrl);
                body.put("nickname", nickname);

                if(appleRefreshToken != null) {
                    body.put("appleRefreshToken", appleRefreshToken);
                }

                String key = redisClient.setTicketData("ti", objectMapper.writeValueAsString(body));
                log.info("key = {}, body = {}", key, objectMapper.writeValueAsString(body));

                targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                        .queryParam("key", key)
                        .build().toString();
            }
* */