package triB.triB.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import triB.triB.auth.dto.*;
import triB.triB.auth.service.AuthService;
import triB.triB.auth.service.MailService;
import triB.triB.global.response.ApiResponse;
import triB.triB.user.dto.PasswordRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 이메일로 인증번호 보내기
     */
    @PostMapping("/email/{email}")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@PathVariable("email") @Email String email) {
        authService.sendCodeToEmail(email);
        return ApiResponse.ok("인증번호를 보냈습니다.", null);
    }

    /**
     * 이메일로 보낸 인증번호 확인
     */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody EmailRequest emailRequest) {
        authService.verifiedCode(emailRequest.getEmail(), emailRequest.getCode());
        return ApiResponse.ok("인증번호가 일치합니다.", null);
    }

    /**
     * 아이디 중복 확인
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Void>> checkUsername(@RequestParam("username") String username) {
        authService.duplicateUsername(username);
        return ApiResponse.ok("사용 가능한 아이디입니다.", null);
    }

    /**
     * 이메일로 회원가입
     */
    @PostMapping(value = "/email",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> signup(
            @RequestPart(name = "photo", required = false) MultipartFile photo,
            @RequestPart(name = "meta", required = true) AuthRequest authRequest
    ) {
        authService.signup(photo, authRequest);
        return ApiResponse.created("회원가입 성공", null);
    }

    /**
     * 이메일로 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse response =  authService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
        return ApiResponse.ok("로그인 성공", response);
    }

    /**
     * 신규 소셜 로그인 유저 회원가입 & 로그인
     */
    @PostMapping(value = "/complete-profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> completeProfile(
            @RequestPart(name = "photo", required = false) MultipartFile photo,
            @RequestPart(name = "meta", required = true) RegisterRequest registerRequest
    ) {
        log.info("complete profile 요청");
        Long userId = authService.socialSignup(photo, registerRequest);
        AuthResponse response = authService.socialLogin(userId);
        return ApiResponse.created("소셜로그인 회원가입을 성공했습니다.", response);
    }

    /**
     * 리프레스 토큰으로 accessToken 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        AccessTokenResponse response = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
        return ApiResponse.ok("accessToken을 재발급 받았습니다.", response);
    }

    /**
     * 소셜 로그인해서 발급받은 티켓 아이디로 정보 조회
     */
    @GetMapping("/ott/exchange")
    public ResponseEntity<ApiResponse<Map<String, Object>>> socialLogin(@RequestParam String key) throws JsonProcessingException {
        Map<String, Object> data = authService.getBodyByTicket(key);
        return ApiResponse.ok("소셜로그인에 성공했습니다.", data);
    }

    @PostMapping("/password/{email}")
    public ResponseEntity<ApiResponse<Void>> password(@PathVariable("email") @Email String email) {
        authService.sendPasswordToEmail(email);
        return ApiResponse.ok("임시 비밀번호를 이메일로 발급했습니다. 로그인 후 비밀번호를 변경해주세요.", null);
    }

    @PostMapping("/apple/complete")
    public ResponseEntity<ApiResponse<AuthResponse>> completeApple(
            @RequestBody AppleCompleteRequest appleCompleteRequest
    ){
        AuthResponse response = authService.completeAppleSign(appleCompleteRequest.getRegisterToken());
        return ApiResponse.created("소셜로그인 회원가입을 성공했습니다.", response);
    }
}



