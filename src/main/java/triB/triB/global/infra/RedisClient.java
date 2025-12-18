package triB.triB.global.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisClient {

    private final StringRedisTemplate redisTemplate;

    public String getData(String prefix, String key){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(prefix + ":" + key);
    }

    public void setData(String prefix, String key, String value, long duration){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(prefix + ":" + key, value, Duration.ofSeconds(duration));
    }

    public void deleteData(String prefix, String key){
        redisTemplate.delete(prefix + ":" + key);
    }

    public String setTicketData(String prefix, String body){
        String key = UUID.randomUUID().toString();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(prefix + ":" + key, body, Duration.ofSeconds(300));
        return key;
    }

    public Boolean setIfAbsent(String prefix, String key, String value, long duration){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.setIfAbsent(prefix + ":" + key, value, Duration.ofSeconds(duration));
    }

    // 회원가입 진행중인 소셜로그인
    public void savePendingSignup(String provider, String providerId, String refreshToken) {
        setData("pending", provider +"|"+ providerId, refreshToken, 300);
    }

    // 회원가입 끝낸 소셜로그인 (약관 동의 완료 시)
    public void deletePendingSignup(String provider, String providerId) {
        deleteData("pending", provider +"|"+ providerId);
    }

    // pending으로 시작하는 모든 데이터 가져옴
    public java.util.Set<String> getPendingKeys() {
        return redisTemplate.keys("pending:*");
    }

    /**
     * 특정 키의 남은 TTL 조회 (초 단위)
     */
    public Long getTimeToLive(String key) {
        return redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 특정 키의 value 조회 (prefix 없이 직접 조회)
     */
    public String getValueByFullKey(String fullKey) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(fullKey);
    }

    /**
     * 특정 키 삭제 (prefix 없이 직접 삭제)
     */
    public void deleteByFullKey(String fullKey) {
        redisTemplate.delete(fullKey);
    }

}
