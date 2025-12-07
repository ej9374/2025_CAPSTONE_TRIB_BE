package triB.triB.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import triB.triB.auth.dto.AccessTokenResponse;
import triB.triB.auth.dto.AuthRequest;
import triB.triB.auth.dto.AuthResponse;
import triB.triB.auth.dto.RegisterRequest;
import triB.triB.auth.entity.OauthAccount;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.OauthAccountRepository;
import triB.triB.auth.repository.TokenRepository;
import triB.triB.auth.repository.UserRepository;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.infra.RedisClient;
import triB.triB.global.infra.AwsS3Client;
import triB.triB.global.security.JwtProvider;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final MailService mailService;
    private final UserRepository userRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisClient redisClient;
    private final AwsS3Client s3Client;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private static final String charPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String passwordCharPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ?%!#*";


    public void sendCodeToEmail(String email) {
        if (userRepository.existsByEmail(email))
            throw new DataIntegrityViolationException("이미 사용 중인 이메일입니다.");
        String title = "TriB 회원가입 이메일 인증 번호";
        log.info("전송해야될 메일: {}", email);

        if (redisClient.getData("ev", email) != null)
            redisClient.deleteData("ev",email);

        String authCode = createCode();
        log.info("생성된 인증번호: {}", authCode);
        String content = MailTemplate.signupCode(authCode);
        mailService.sendEmail(email, title, content);
        redisClient.setData("ev", email, authCode, 600);
    }

    private String createCode(){
        int length = 6;
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int index = sr.nextInt(charPool.length());
                code.append(charPool.charAt(index));
            }
            return code.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("인증번호 생성을 실패했습니다.");
        }
    }

    public void verifiedCode(String email, String code) {
        log.info("이메일 인증번호 확인");
        String upperCode = code.toUpperCase();
        String savedCode = redisClient.getData("ev", email);
        if (savedCode == null || !savedCode.equals(upperCode)) {
            throw new RuntimeException("인증번호가 일치하지 않습니다.");
        }
        redisClient.deleteData("ev",email);
    }

    public void duplicateUsername(String username) {
        if (userRepository.existsByUsername(username))
            throw new DataIntegrityViolationException("이미 존재하는 아이디입니다.");
    }

    @Transactional
    public void signup(MultipartFile photo, AuthRequest authRequest) {
        log.info("회원가입을 시작합니다.");
        String photoUrl = null;
        try {
            photoUrl = (photo != null && !photo.isEmpty()) ? s3Client.uploadFile(photo) : null;
            User user = User.builder()
                    .photoUrl(photoUrl)
                    .email(authRequest.getEmail())
                    .username(authRequest.getUsername())
                    .password(passwordEncoder.encode(authRequest.getPassword()))
                    .nickname(authRequest.getNickname())
                    .build();
            userRepository.save(user);
            log.info("회원가입이 완료되었습니다. userId ="+ user.getUserId());
        } catch (DataIntegrityViolationException e ){
            // 회원가입 실패시 S3에 올라간 사진 삭제로 무결성 유지
            if (photoUrl != null)
                s3Client.delete(photoUrl);
            throw new DataIntegrityViolationException("이미 존재하는 값입니다.", e);
        }
    }

    @Transactional
    public AuthResponse loginUser(String email, String password) {
        log.info("로그인 시;");
        User user = userRepository.findByEmail(email)
                .orElseThrow(()->
                        new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.info("비밀번호가 일치하지 않습니다.");
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        log.info("로그인한 유저의 정보: userId ="+ user.getUserId()+ " username =" +user.getUsername());
        Long userId = user.getUserId();
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        return new AuthResponse(userId, accessToken, refreshToken);
    }

    @Transactional
    public Long socialSignup(MultipartFile photo, RegisterRequest registerRequest) {
        String registerToken = registerRequest.getRegisterToken();
        String provider = jwtProvider.getProviderFromRegisterToken(registerToken);
        String providerId = jwtProvider.getProviderUserIdFromRegisterToken(registerToken);
        String image = registerRequest.getPhotoUrl();
        String refreshToken = null;
        if (registerRequest.getRefreshToken() != null && !registerRequest.getRefreshToken().isEmpty())
            refreshToken = registerRequest.getRefreshToken();

        log.info("registerToken = " + registerToken + " providerId = " + providerId + " image = " + image);

        String uploadUrl = null;
        String photoUrl = null;

        try {
            if (photo != null && !photo.isEmpty()) {
                uploadUrl = s3Client.uploadFile(photo);
                photoUrl = uploadUrl;
            }
            else if (image != null && !image.isEmpty())
                photoUrl = image;

            User user = User.builder()
                    .photoUrl(photoUrl)
                    .email(null)
                    .username(registerRequest.getUsername())
                    .password(null)
                    .nickname(registerRequest.getNickname())
                    .build();
            userRepository.save(user);

            OauthAccount account = OauthAccount.builder()
                    .user(user)
                    .provider(provider)
                    .providerUserId(providerId)
                    .refreshToken(refreshToken)
                    .build();
            oauthAccountRepository.save(account);
            return user.getUserId();
        } catch (DataIntegrityViolationException e ){
            // 회원가입 실패시 S3에 올라간 사진 삭제로 무결성 유지
            if (uploadUrl != null)
                s3Client.delete(uploadUrl);
            throw new DataIntegrityViolationException("회원가입을 실패했습니다.", e);
        }
    }

    public AuthResponse socialLogin(Long userId){
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        return new AuthResponse(userId, accessToken, refreshToken);
    }

    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)){
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId = jwtProvider.extractUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        log.info("redis에서 refreshToken 존재하는지 조회");
        if (!redisClient.getData("rf", String.valueOf(userId)).equals(refreshToken)) {
            log.info("redis에 refreshToken 없음");
            redisClient.deleteData("rf", String.valueOf(userId));
            throw new JwtException("토큰이 만료되었거나 토큰이 유효하지 않습니다.");
        }
        String accessToken = jwtProvider.generateAccessToken(userId);
        return new AccessTokenResponse(accessToken);
    }

    public Map<String, Object> getBodyByTicket(String key) throws JsonProcessingException {
        String str = redisClient.getData("ti", key);
        if (str == null)
            throw new EntityNotFoundException("해당 키가 존재하지 않습니다. 다시 로그인해주세요");
        return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
    }

    public void sendPasswordToEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이메일입니다."));

        String title = "TriB 임시 비밀번호 생성";
        log.info("전송해야될 메일: {}", email);

        if (redisClient.getData("ev", email) != null)
            redisClient.deleteData("ev",email);

        String temporaryPassword = createTemporaryPassword();
        log.info("생성된 임시 비밀번호: {}", temporaryPassword);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);

        String content = MailTemplate.newPassword(temporaryPassword);
        mailService.sendEmail(email, title, content);
    }

    public String createTemporaryPassword(){
        int length = 8;
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int index = sr.nextInt(passwordCharPool.length());
                code.append(passwordCharPool.charAt(index));
            }
            return code.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("임시 비밀번호 생성을 실패했습니다.");
        }
    }
}
