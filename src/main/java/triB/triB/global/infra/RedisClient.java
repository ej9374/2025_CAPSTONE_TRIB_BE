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

}
