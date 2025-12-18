package triB.triB.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis")
    public RedisProps redisProps() {
        return new RedisProps();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProps props) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(
                props.getHost(), props.getPort());
        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            cfg.setPassword(props.getPassword());
        }
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(LettuceConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    public static class RedisProps {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
