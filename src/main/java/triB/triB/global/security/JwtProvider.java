package triB.triB.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import triB.triB.global.infra.RedisClient;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.access-secret}")
    private String accessKeyString;

    @Value("${jwt.refresh-secret}")
    private String refreshKeyString;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpiresIn;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpiresIn;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    private final RedisClient redisClient;

    @PostConstruct
    public void init() {
        if (accessKeyString == null || refreshKeyString == null) {
            throw new JwtException("jwt 비밀키가 존재하지 않습니다.");
        }
        byte[] accessKeyBytes = Base64.getDecoder().decode(accessKeyString);
        byte[] refreshKeyBytes = Base64.getDecoder().decode(refreshKeyString);
        this.accessKey = Keys.hmacShaKeyFor(accessKeyBytes);
        this.refreshKey = Keys.hmacShaKeyFor(refreshKeyBytes);
    }

    public String generateRegisterToken(String provider, String providerUserId, String photoUrl, String nickname, String username, String appleRefreshToken) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + 5 * 60 * 1000);

        log.info("provider = {}, providerUserId = {}, photoUrl = {}, nickname = {}", provider, providerUserId,  photoUrl, nickname);

        return Jwts.builder()
                .claim("provider", provider)
                .claim("providerUserId", providerUserId)
                .claim("photoUrl", photoUrl)
                .claim("nickname", nickname)
                .claim("username", username)
                .claim("appleRefreshToken", appleRefreshToken)
                .issuedAt(now)
                .expiration(expires)
                .signWith(accessKey)
                .compact();
    }

    public String getProviderFromRegisterToken(String token) {
        return tokenParser(token).get("provider", String.class);
    }

    public String getProviderUserIdFromRegisterToken(String token) {
        return tokenParser(token).get("providerUserId", String.class);
    }

    public String getPhotoUrlFromRegisterToken(String token){
        return tokenParser(token).get("photoUrl", String.class);
    }

    public String getNicknameFromRegisterToken(String Token){
        return tokenParser(Token).get("nickname", String.class);
    }

    public String getUsernameFromRegisterToken(String Token){
        return tokenParser(Token).get("username", String.class);
    }

    public String getAppleRefreshTokenFromRegisterToken(String Token){
        return tokenParser(Token).get("appleRefreshToken", String.class);
    }

    private Claims tokenParser(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(null, null, "토큰이 만료되었습니다. 다시 발급받아주세요.");
        } catch (Exception e) {
            throw new JwtException("토큰이 유효하지 않습니다.");
        }
    }

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + accessExpiresIn);
        return Jwts.builder()
                .subject(userId.toString())
                .expiration(expires)
                .issuedAt(now)
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + refreshExpiresIn);
        String refreshToken =  Jwts.builder()
                .subject(userId.toString())
                .expiration(expires)
                .issuedAt(now)
                .signWith(refreshKey)
                .compact();
        redisClient.setData("rf", userId.toString(), refreshToken, refreshExpiresIn);
        return refreshToken;
    }

    public boolean validateAccessToken(String accessToken) {
        return validateTokenInternal(accessToken, accessKey);
    }

    public boolean validateRefreshToken(String refreshToken) {
        return validateTokenInternal(refreshToken, refreshKey);
    }

    private boolean validateTokenInternal(String token, SecretKey key){
        try {
            Jwts.parser().verifyWith(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e){
            log.warn("Jwt token이 만료되었습니다.: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e){
            log.warn("Jwt token이 올바르지 않습니다.: {}", e.getMessage());
        }
        return false;
    }

    public Long extractUserId(String accessToken) {
        return Long.valueOf(getClaimsFromAccessToken(accessToken).getSubject());
    }

    public Long extractUserIdFromRefreshToken(String refreshToken) {
        return Long.valueOf(getClaimsFromRefreshToken(refreshToken).getSubject());
    }

    public Claims getClaimsFromAccessToken(String accessToken) {
        return getClaimsInternal(accessToken, accessKey);
    }

    public Claims getClaimsFromRefreshToken(String refreshToken) {
        return getClaimsInternal(refreshToken, refreshKey);
    }

    private Claims getClaimsInternal(String token, SecretKey key) {
        try {
            return Jwts.parser().verifyWith(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Jwt token이 만료되었습니다.: {}", e.getMessage());
            return e.getClaims();
        }
    }
}
