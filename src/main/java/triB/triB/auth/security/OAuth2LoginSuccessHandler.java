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
