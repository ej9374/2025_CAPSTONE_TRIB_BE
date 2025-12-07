package triB.triB.auth.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class AppleClientSecretGenerator {

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    @Value("${apple.team.id}")
    private String teamId;

    @Value("${apple.login.key}")
    private String keyId;

    @Value("${apple.key.path}")
    private String keyPath;

    public String generateAppleClientSecret() throws Exception {
        Instant now = Instant.now();
        Instant expiration = now.plus(180, ChronoUnit.DAYS); // 6개월

        return Jwts.builder()
                .header()
                .add("kid", keyId)
                .add("alg", "ES256")
                .and()
                .issuer(teamId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(Date.from(expiration))
                .audience().add("https://appleid.apple.com").and()
                .subject(clientId)
                .signWith(getPrivateKey())
                .compact();
    }


    private PrivateKey getPrivateKey() throws Exception {

        try (FileInputStream resource = new FileInputStream(keyPath);
             Reader reader = new InputStreamReader(resource);
             PEMParser pemParser = new PEMParser(reader)) {

            Object pemObject = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (pemObject instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) pemObject);
            }

            throw new IllegalArgumentException("Invalid private key format");
        }
    }
}
