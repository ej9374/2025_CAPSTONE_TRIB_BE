package triB.triB.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();

        // 1. SameSite=None 설정 (핵심)
        serializer.setSameSite("None");

        // 2. Secure 강제 설정 (HTTPS 환경 필수)
        serializer.setUseSecureCookie(true);

        // 3. 쿠키 경로를 루트로 설정
        serializer.setCookiePath("/");

        return serializer;
    }
}
